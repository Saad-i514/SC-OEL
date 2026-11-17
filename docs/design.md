# Implementation record

Source: 30B.pdf. Java Swing chosen to satisfy the desktop Java requirement without a UI dependency. ArrayList holds each fetched inventory snapshot; PostgreSQL stores durable records. Prepared statements perform CRUD by primary key. Stock status is explicitly selected, because the brief does not define automatic thresholds.

Each feature is recorded separately in Git. No existing inventory data is reset or seeded.
