import pkg from 'pg';
const { Pool } = pkg;
import jwt from 'jsonwebtoken';

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();

  const client = await pool.connect();

  try {
    // Ensure listings table exists
    // We use the same structure as products but linked to a user
    await client.query(`
      CREATE TABLE IF NOT EXISTS listings (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id),
        name TEXT NOT NULL,
        price TEXT NOT NULL,
        description TEXT,
        imageUrl TEXT,
        category TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    if (req.method === 'GET') {
      const result = await client.query('SELECT * FROM listings ORDER BY created_at DESC');
      client.release();
      return res.status(200).json(result.rows);
    }

    if (req.method === 'POST') {
      const authHeader = req.headers.authorization;
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        client.release();
        return res.status(401).json({ error: 'Unauthorized. Please login to post products.' });
      }

      const token = authHeader.split(' ')[1];
      let decoded;
      try {
        decoded = jwt.verify(token, process.env.JWT_SECRET || 'bex-market-secret-key');
      } catch (e) {
        client.release();
        return res.status(401).json({ error: 'Invalid session. Please login again.' });
      }

      const { name, price, description, imageUrl, category } = req.body;

      if (!name || !price) {
        client.release();
        return res.status(400).json({ error: 'Name and price are required' });
      }

      const result = await client.query(
        'INSERT INTO listings (user_id, name, price, description, imageUrl, category) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
        [decoded.userId, name, price, description, imageUrl, category]
      );

      // Also insert into products table if it exists to show globally
      try {
        await client.query(
          'INSERT INTO products (name, price, description, "imageUrl") VALUES ($1, $2, $3, $4)',
          [name, price, description, imageUrl]
        );
      } catch (e) {
        console.warn('Could not mirror to products table:', e.message);
      }

      client.release();
      return res.status(201).json(result.rows[0]);
    }

    client.release();
    return res.status(405).json({ error: 'Method not allowed' });
  } catch (err) {
    client.release();
    console.error('Listings error:', err);
    res.status(500).json({ error: 'Internal Server Error', message: err.message });
  }
}
