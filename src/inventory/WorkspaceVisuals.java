package inventory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Painted presentation components for the Stockroom desktop workspace.
 * @author OEL Team */
final class WorkspaceVisuals {
    private WorkspaceVisuals() { }

    static JPanel rounded(Color background, int radius) {
        return new JPanel() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); setBackground(background); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getBackground()); g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g.dispose();
                super.paintComponent(graphics);
            }
        };
    }

    static JButton button(String title, Color background, Color foreground) {
        var button = new JButton(title) {
            private static final long serialVersionUID = 1L;
            private float hover;
            private final Timer timer = new Timer(16, event -> {
                float target = getModel().isRollover() && isEnabled() ? 1 : 0;
                hover += (target - hover) * .3f;
                if (Math.abs(target - hover) < .02f) { hover = target; ((Timer) event.getSource()).stop(); }
                repaint();
            });
            {
                setContentAreaFilled(false); setOpaque(false); setRolloverEnabled(true);
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent event) { if (!Boolean.FALSE.equals(UIManager.get("Stockroom.motion"))) timer.start(); }
                    @Override public void mouseExited(MouseEvent event) { timer.start(); }
                });
            }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getBackground();
                g.setColor(base); g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g.setColor(new Color(128, 150, 160, (int) (hover * 35))); g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (isFocusOwner()) { g.setColor(new Color(70, 175, 156)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(2, 2, getWidth()-5, getHeight()-5, 10, 10); }
                g.dispose(); super.paintComponent(graphics);
            }
            @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
        };
        button.setBackground(background); button.setForeground(foreground); button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16)); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button;
    }

    static final class Pages extends JPanel {
        private static final long serialVersionUID = 1L;
        private final CardLayout cards = new CardLayout();
        private float progress = 1;
        private long started;
        private boolean animated = true;
        private final Timer timer = new Timer(16, event -> {
            progress = Math.min(1f, (System.nanoTime() - started) / 220_000_000f);
            if (progress == 1) ((Timer) event.getSource()).stop();
            repaint();
        });
        Pages() { setLayout(cards); setOpaque(false); }
        void showPage(String page) {
            cards.show(this, page); timer.stop(); progress = animated ? 0 : 1;
            if (animated) { started = System.nanoTime(); timer.start(); } repaint();
        }
        void setAnimated(boolean enabled) { animated = enabled; if (!enabled) { timer.stop(); progress = 1; repaint(); } }
        @Override protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            float eased = 1 - (1-progress)*(1-progress);
            g.setComposite(AlphaComposite.SrcOver.derive(.35f + .65f * eased)); g.translate(0, (int)((1-eased)*10));
            super.paintChildren(g); g.dispose();
        }
        @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
    }

    static final class Artwork extends JComponent {
        private static final long serialVersionUID = 1L;
        private final transient BufferedImage image;
        Artwork(String filename) {
            BufferedImage loaded;
            try { loaded = ImageIO.read(Path.of("assets", filename).toFile()); }
            catch (IOException exception) { loaded = null; }
            image = loaded; setPreferredSize(new Dimension(330, 190));
            setToolTipText("Stockroom warehouse illustration");
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) return;
            Graphics2D g = (Graphics2D) graphics.create(); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale = Math.min((double)getWidth()/image.getWidth(), (double)getHeight()/image.getHeight());
            int w=(int)(image.getWidth()*scale), h=(int)(image.getHeight()*scale);
            g.drawImage(image,(getWidth()-w)/2,(getHeight()-h)/2,w,h,null); g.dispose();
        }
    }

    static final class StockChart extends JComponent {
        private static final long serialVersionUID = 1L;
        private long[] values = {0, 0, 0};
        StockChart() { setPreferredSize(new Dimension(170, 160)); }
        void setValues(long in, long low, long out) { values = new long[]{in,low,out}; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int size=Math.min(getWidth()-18,getHeight()-18), x=(getWidth()-size)/2, y=(getHeight()-size)/2;
            long total=values[0]+values[1]+values[2]; Color[] colors={new Color(0,128,111),new Color(230,161,56),new Color(210,83,96)};
            g.setColor(new Color(232,237,241)); g.fillOval(x,y,size,size);
            double start=90;
            for(int i=0;i<values.length;i++) { double angle=total==0?0:360.0*values[i]/total; g.setColor(colors[i]); g.fill(new java.awt.geom.Arc2D.Double(x,y,size,size,start,-angle,java.awt.geom.Arc2D.PIE)); start-=angle; }
            g.setColor(Color.WHITE); int inset=size/5; g.fillOval(x+inset,y+inset,size-2*inset,size-2*inset);
            g.setColor(new Color(29,43,62)); g.setFont(new Font("Segoe UI",Font.BOLD,24)); String number=Long.toString(total);
            g.drawString(number,(getWidth()-g.getFontMetrics().stringWidth(number))/2,getHeight()/2+4);
            g.setFont(new Font("Segoe UI",Font.PLAIN,11)); g.drawString("products",(getWidth()-g.getFontMetrics().stringWidth("products"))/2,getHeight()/2+23); g.dispose();
        }
    }
}
