package edu.cmu.cs.diamond.pathfind;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cmu.cs.diamond.wholeslide.gui.WholeslideView;

public class PairedSlideView extends JPanel {

	private final JToggleButton linkButton;
	private final WholeslideView slides[] = new WholeslideView[2];
	private final JPanel slideViews;

	public PairedSlideView() {
		setLayout(new BorderLayout());
		
		// main view in center
        slideViews = new JPanel(new GridLayout(1, 2));
        add(slideViews);

        // link button at bottom
        linkButton = new JToggleButton("Link");
        add(linkButton, BorderLayout.SOUTH);
        linkButton.setVisible(false);
        linkButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (linkButton.isSelected()) {
                    slides[0].linkWithOther(slides[1]);
                } else {
                    slides[0].unlinkOther();
                }
            }
        });
	}

	public WholeslideView getLeftSlide() {
		return slides[0];
	}

	public void setLeftSlide(WholeslideView wv) {
        WholeslideView oldSlide = slides[0];
        if (oldSlide != null) {
            oldSlide.unlinkOther();
        }

        slides[0] = wv;
        
        slideViews.add(wv, 0);
	}
	
	public void setRightSlide(WholeslideView wv) {
        if (wv == null) {
            linkButton.setVisible(false);
        } else {
            slides[1] = wv;
            slideViews.add(slides[1]);
            linkButton.setVisible(true);
        }
        
        slideViews.revalidate();
	}
}
