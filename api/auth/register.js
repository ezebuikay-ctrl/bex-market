import pkg from 'pg';
const { Pool } = pkg;
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

export default async function handler(req, res) {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  // Debug log for Vercel
  console.log('Registration request body:', JSON.stringify(req.body));

  const { email, password, name, username, fullname } = req.body;

  // Accept various field names commonly used in frontends
  const userEmail = email;
  const userPassword = password;
  const userName = name || username || fullname || 'User';

  if (!userEmail || !userPassword) {
    return res.status(400).json({
      error: 'Missing fields',
      receivedFields: Object.keys(req.body || {}),
      message: 'Email and password are required'
    });
  }

  try {
    const client = await pool.connect();

    // Create users table if not exists
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        email TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        name TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Check if user exists
    const userCheck = await client.query('SELECT * FROM users WHERE email = $1', [userEmail]);
    if (userCheck.rows.length > 0) {
      client.release();
      return res.status(400).json({ error: 'User already exists' });
    }

    const hashedPassword = await bcrypt.hash(userPassword, 10);
    const result = await client.query(
      'INSERT INTO users (email, password, name) VALUES ($1, $2, $3) RETURNING id, email, name',
      [userEmail, hashedPassword, userName]
    );

    const user = result.rows[0];
    client.release();

    const token = jwt.sign(
      { userId: user.id },
      process.env.JWT_SECRET || 'bex-market-secret-key',
      { expiresIn: '30d' }
    );

    res.status(201).json({ user, token });
  } catch (err) {
    console.error('Registration error details:', err);
    res.status(500).json({
      error: 'Internal Server Error',
      message: err.message,
      code: err.code
    });
  }
}
