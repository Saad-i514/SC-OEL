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

    static final class AttentionRow extends JPanel implements ListCellRenderer<Item> {
        private static final long serialVersionUID = 1L;
        private final JLabel name = new JLabel(), quantity = new JLabel(), badge = new JLabel();
        AttentionRow() {
            super(new BorderLayout(12, 0)); setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(237,231,244)),BorderFactory.createEmptyBorder(8,4,8,8)));
            var details = new JPanel(new GridLayout(2,1,0,3)); details.setOpaque(false);
            name.setFont(new Font("Segoe UI",Font.BOLD,12)); name.setForeground(new Color(43,32,69));
            quantity.setFont(new Font("Segoe UI",Font.PLAIN,11)); quantity.setForeground(new Color(111,100,130));
            details.add(name); details.add(quantity); add(details,BorderLayout.CENTER);
            badge.setFont(new Font("Segoe UI",Font.BOLD,10)); badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setOpaque(true); badge.setPreferredSize(new Dimension(104,24));
            var badgeHolder = new JPanel(new GridBagLayout()); badgeHolder.setOpaque(false); badgeHolder.add(badge); add(badgeHolder,BorderLayout.EAST);
        }
        @Override public Component getListCellRendererComponent(JList<? extends Item> list,Item item,int index,boolean selected,boolean focus) {
            name.setText(item.name()); quantity.setText(String.format("%,d %s remaining",item.quantity(),item.quantity()==1?"unit":"units"));
            boolean out = item.status().equals("Out of Stock"); badge.setText(item.status());
            badge.setBackground(out?new Color(251,225,235):new Color(255,237,217));
            badge.setForeground(out?new Color(160,46,84):new Color(136,82,20));
            setToolTipText(item.name()+" — "+quantity.getText()+" — "+item.status());
            return this;
        }
    }

    static JPanel rounded(Color background, int radius) {
        return new JPanel() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); setBackground(background); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(38,25,74,10)); g.fillRoundRect(2,4,getWidth()-4,getHeight()-5,radius,radius);
                g.setPaint(new GradientPaint(0,0,getBackground(),getWidth(),getHeight(),blend(getBackground(),new Color(231,225,246),.18f)));
                g.fillRoundRect(1,1,getWidth()-3,getHeight()-6,radius,radius);
                g.setColor(new Color(113,94,150,30)); g.drawRoundRect(1,1,getWidth()-3,getHeight()-6,radius,radius); g.dispose();
                super.paintComponent(graphics);
            }
        };
    }

    private static Color blend(Color a, Color b, float amount) {
        return new Color((int)(a.getRed()*(1-amount)+b.getRed()*amount), (int)(a.getGreen()*(1-amount)+b.getGreen()*amount), (int)(a.getBlue()*(1-amount)+b.getBlue()*amount));
    }

    static JButton button(String title, Color background, Color foreground) {
        return new MagneticButton(title, background, foreground);
    }

    static final class MagneticButton extends JButton {
        private static final long serialVersionUID = 1L;
        float offsetX, offsetY;
        private float targetX, targetY, hover;
        private final Timer timer = new Timer(16, event -> {
            boolean motion = !Boolean.FALSE.equals(UIManager.get("Stockroom.motion")) && isEnabled();
            float x = motion ? targetX : 0, y = motion ? targetY : 0;
            float glow = getModel().isRollover() && isEnabled() ? 1 : 0;
            offsetX += (x-offsetX)*.24f; offsetY += (y-offsetY)*.24f; hover += (glow-hover)*.24f;
            if (Math.abs(x-offsetX)<.03f && Math.abs(y-offsetY)<.03f && Math.abs(glow-hover)<.03f) {
                offsetX=x; offsetY=y; hover=glow; ((Timer)event.getSource()).stop();
            }
            repaint();
        });
        MagneticButton(String title, Color background, Color foreground) {
            super(title); setBackground(background); setForeground(foreground); setFont(new Font("Segoe UI",Font.BOLD,13));
            setBorder(BorderFactory.createEmptyBorder(13,18,13,18)); setContentAreaFilled(false); setOpaque(false); setRolloverEnabled(true); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            var mouse=new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent event) {
                    targetX=Math.max(-3,Math.min(3,(event.getX()-getWidth()/2f)*.08f));
                    targetY=Math.max(-2,Math.min(2,(event.getY()-getHeight()/2f)*.12f)); timer.start();
                }
                @Override public void mouseEntered(MouseEvent event) { timer.start(); }
                @Override public void mouseExited(MouseEvent event) { targetX=0; targetY=0; timer.start(); }
            };
            addMouseListener(mouse); addMouseMotionListener(mouse);
            addPropertyChangeListener("enabled",event->{ if(!isEnabled()) resetMotion(); });
        }
        void resetMotion() { timer.stop(); targetX=0; targetY=0; offsetX=0; offsetY=0; hover=0; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(offsetX,offsetY+(getModel().isPressed()?1:0));
            Color base=isEnabled()?getBackground():blend(getBackground(),new Color(241,238,248),.65f);
            g.setColor(new Color(38,23,67,(int)(18+hover*16))); g.fillRoundRect(4,5,getWidth()-8,getHeight()-7,16,16);
            g.setPaint(new GradientPaint(0,0,blend(base,Color.WHITE,.08f+hover*.1f),getWidth(),getHeight(),base));
            g.fillRoundRect(3,2,getWidth()-7,getHeight()-7,15,15);
            g.setColor(new Color(130,109,170,55)); g.drawRoundRect(3,2,getWidth()-7,getHeight()-7,15,15);
            if(isFocusOwner()) { g.setColor(new Color(155,118,249)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,18,18); }
            g.dispose();
            Graphics2D label=(Graphics2D)graphics.create(); label.translate(offsetX,offsetY); super.paintComponent(label); label.dispose();
        }
        @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
    }

    static void setMotion(boolean enabled, Container root) {
        UIManager.put("Stockroom.motion", enabled);
        for (Component component : root.getComponents()) {
            if (!enabled && component instanceof MagneticButton button) button.resetMotion();
            if (component instanceof Container child) setMotion(enabled, child);
        }
    }

    static void styleScrollbars(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JScrollBar bar) {
                bar.setPreferredSize(bar.getOrientation() == SwingConstants.VERTICAL ? new Dimension(9, 0) : new Dimension(0, 9));
                bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                    @Override protected JButton createDecreaseButton(int direction) { return hiddenArrow(); }
                    @Override protected JButton createIncreaseButton(int direction) { return hiddenArrow(); }
                    private JButton hiddenArrow() { var button=new JButton(); button.setPreferredSize(new Dimension()); return button; }
                    @Override protected void paintTrack(Graphics graphics,JComponent c,Rectangle bounds) {
                        graphics.setColor(new Color(247,244,251)); graphics.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
                    }
                    @Override protected void paintThumb(Graphics graphics,JComponent c,Rectangle bounds) {
                        Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(isThumbRollover()?new Color(153,116,209):new Color(203,183,229));
                        g.fillRoundRect(bounds.x+1,bounds.y+1,Math.max(0,bounds.width-2),Math.max(0,bounds.height-2),8,8); g.dispose();
                    }
                });
            } else if (component instanceof Container child) styleScrollbars(child);
        }
    }

    static JTextField input(String initial) {
        var field=new JTextField(initial) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics graphics) {
                paintField(this,graphics,isFocusOwner()); super.paintComponent(graphics);
            }
        };
        field.setOpaque(false); field.setForeground(new Color(43,32,69)); field.setCaretColor(new Color(116,68,218));
        field.setBorder(BorderFactory.createEmptyBorder(10,13,10,13));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent event) { field.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent event) { field.repaint(); }
        });
        return field;
    }
    private static void paintField(JComponent component, Graphics graphics, boolean focused) {
        Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(component.isEnabled()?new Color(249,247,253):new Color(242,240,246)); g.fillRoundRect(1,1,component.getWidth()-3,component.getHeight()-3,12,12);
        g.setColor(focused?new Color(131,86,220):new Color(216,207,232)); g.setStroke(new BasicStroke(focused?2:1));
        g.drawRoundRect(1,1,component.getWidth()-3,component.getHeight()-3,12,12); g.dispose();
    }
    static JComboBox<String> select(String[] values) {
        var combo=new JComboBox<String>(values) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics graphics) { paintField(this,graphics,hasFocus()); super.paintComponent(graphics); }
        };
        combo.setOpaque(false); combo.setBackground(new Color(249,247,253)); combo.setForeground(new Color(43,32,69));
        combo.setBorder(BorderFactory.createEmptyBorder(3,8,3,5));
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                return new JButton() {
                    private static final long serialVersionUID = 1L;
                    { setOpaque(false); setContentAreaFilled(false); setBorder(null); }
                    @Override protected void paintComponent(Graphics graphics) {
                        Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(new Color(116,68,218)); g.setStroke(new BasicStroke(1.8f)); int x=getWidth()/2,y=getHeight()/2;
                        g.drawLine(x-4,y-2,x,y+2); g.drawLine(x,y+2,x+4,y-2); g.dispose();
                    }
                };
            }
            @Override public void paintCurrentValueBackground(Graphics g,Rectangle bounds,boolean focus) { }
        });
        combo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                super.getListCellRendererComponent(list,value,index,selected,focus); setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
                setBackground(selected?new Color(234,225,251):new Color(249,247,253)); setForeground(new Color(43,32,69)); return this;
            }
        });
        return combo;
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
            g.clip(new java.awt.geom.RoundRectangle2D.Double((getWidth()-w)/2.0,(getHeight()-h)/2.0,w,h,24,24));
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
            long total=values[0]+values[1]+values[2]; Color[] colors={new Color(119,76,210),new Color(229,160,87),new Color(204,91,128)};
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
