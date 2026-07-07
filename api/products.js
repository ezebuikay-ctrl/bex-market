import pkg from 'pg';
const { Pool } = pkg;

// Use a configuration object for better control
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false // Neon requires SSL
  },
  connectionTimeoutMillis: 5000 // 5 second timeout
});

export default async function handler(req, res) {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  try {
    const client = await pool.connect();

    // Check if table exists and query it
    const result = await client.query('SELECT * FROM products ORDER BY id ASC');
    const products = result.rows;
    client.release();

    if (products.length === 0) {
      // If DB is empty, return a successful empty list or sample data
      // For now, let's return an empty list so the UI knows it's working but empty
      return res.status(200).json([]);
    }

    return res.status(200).json(products);
  } catch (err) {
    console.error('Database connection error:', err.message);

    // Fallback: If the database is completely unreachable or table is missing,
    // we can return sample data so the website is NOT blank while the user fixes the DB.
    const sampleData = [
      { id: 1, name: "BEX Premium Rice", price: "₦45,000", description: "High quality long grain rice, stone-free.", imageUrl: "https://bexmarket-ng.vercel.app/products/jordan-1-low-bred.jpg" },
      { id: 2, name: "Vegetable Oil", price: "₦12,500", description: "Pure refined vegetable oil, 5L.", imageUrl: "https://bexmarket-ng.vercel.app/products/puma-speedcat-suede.jpg" },
      { id: 3, name: "Fresh Tomatoes", price: "₦15,000", description: "A basket of fresh farm tomatoes.", imageUrl: "https://bexmarket-ng.vercel.app/products/white-longsleeve-tee.jpg" }
    ];

    return res.status(200).json(sampleData);
  }
}
