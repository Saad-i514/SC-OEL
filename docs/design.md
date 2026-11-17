# Implementation record

Source: 30B.pdf. Java Swing chosen to satisfy the desktop Java requirement without a UI dependency. ArrayList holds each fetched inventory snapshot; PostgreSQL stores durable records. Prepared statements perform CRUD by primary key. Stock status is explicitly selected, because the brief does not define automatic thresholds.

Each feature is recorded separately in Git. No existing inventory data is reset or seeded.


## Multi-page workspace

Overview, Inventory, and Activity use CardLayout. Navigation does not fetch data or discard form fields; New item and Clear explicitly reset the editor. The Inventory view searches names literally (case-insensitive), combines that with a stock-status filter, and converts displayed row indexes back to model indexes before writes. Numeric column classes ensure numeric sorting. Summary totals use long arithmetic and derive from the full fetched snapshot. The chart and attention list use the manually selected stock statuses.

WorkspaceVisuals uses Swing/Java2D for rounded surfaces, hover highlights, and a 220 ms fade/slide transition. Timers stop when the animation finishes or the component is removed. The sidebar animation control supports reduced motion. Artwork is loaded from bundled PNGs; missing files do not block CRUD, and automated checks verify the bundled assets are readable.

Activity records only this session's operation results, newest first. It is explicitly not a persistent audit log. No mock sales, revenue, or trend data is shown. Database credentials and security boundaries remain unchanged.
