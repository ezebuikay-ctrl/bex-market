import pkg from 'pg';
const { Pool } = pkg;
import jwt from 'jsonwebtoken';

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();

  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'bex-market-secret-key');
    const client = await pool.connect();
    const result = await client.query(
      'SELECT * FROM listings WHERE user_id = $1 ORDER BY created_at DESC',
      [decoded.userId]
    );
    client.release();

    res.status(200).json(result.rows);
  } catch (err) {
    console.error('Fetch mine listings error:', err);
    res.status(401).json({ error: 'Invalid token' });
  }
}
