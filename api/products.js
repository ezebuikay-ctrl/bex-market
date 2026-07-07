import pkg from 'pg';
const { Pool } = pkg;

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DATABASE_URL && (process.env.DATABASE_URL.includes('localhost') || process.env.DATABASE_URL.includes('helium')) ? false : {
    rejectUnauthorized: false
  }
});

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  try {
    const client = await pool.connect();
    const result = await client.query('SELECT * FROM products');
    const products = result.rows;
    client.release();

    res.status(200).json(products);
  } catch (err) {
    console.error('Database error:', err);
    res.status(500).json({
      error: 'Internal Server Error',
      message: err.message
    });
  }
}
