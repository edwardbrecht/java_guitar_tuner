import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GUI extends JFrame{
    public JFrame f;
    public JButton b;
    public JLabel l1, l2;

    public GUI() {
        super("Pitch Detector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f = new JFrame("Pitch Detector");

        l1 = new JLabel("AutoCorrelation");
        l2 = new JLabel("FFT");

        b = new JButton("Stop Listening");

        JPanel p = new JPanel();

        p.add(l1);
        p.add(l2);
        p.add(b);

        f.add(p);

        f.setSize(640,480);

        f.setVisible(true);
    }

}
