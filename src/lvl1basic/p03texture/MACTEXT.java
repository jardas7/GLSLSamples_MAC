package lvl1basic.p03texture;

/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */


        import com.jogamp.opengl.*;
        import transforms.Vec2D;

        import java.awt.Font;
        import java.awt.Frame;
        import java.awt.event.WindowAdapter;
        import java.awt.event.WindowEvent;

        import com.jogamp.opengl.awt.GLCanvas;

        import com.jogamp.opengl.util.Animator;
        import com.jogamp.opengl.util.awt.TextRenderer;


/** A simple test of the TextRenderer class. Draws gears underneath
 with moving Java 2D-rendered text on top. */

public class MACTEXT implements GLEventListener {
    public static void main(String[] args) {
        Frame frame = new Frame("Text Renderer Test");
        GLCapabilities caps = new GLCapabilities(GLProfile.getGL2GL3());
        caps.setAlphaBits(8);
        GLCanvas canvas = new GLCanvas(caps);
        canvas.addGLEventListener(new MACTEXT());
        frame.add(canvas);
        frame.setSize(512, 512);
        final Animator animator = new Animator(canvas);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Run this on another thread than the AWT event queue to
                // make sure the call to Animator.stop() completes before
                // exiting
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        animator.stop();
                        System.exit(0);
                    }
                }).start();
            }
        });

        frame.setVisible(true);
        animator.start();
    }

    private TextRenderer renderer;
    private Vec2D position;
    private final String TEST_STRING = "Max na které toto funguje je: "+ GLProfile.getGL2ES1() +" !";

    @Override
    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        // Don't artificially slow us down, at least on platforms where we
        // have control over this (note: on X11 platforms this may not
        // have the effect of overriding the setSwapInterval(1) in the
        // Gears demo)
        gl.setSwapInterval(0);

        renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 24));

        // Start the text half way up the left side
        position = new Vec2D(0.0f, drawable.getSurfaceHeight() / 2);

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        renderer = null;
        position = null;
    }

    @Override
        public void display(GLAutoDrawable drawable) {

            // Prepare to draw text
        renderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        // Draw text
        renderer.draw(TEST_STRING, (int) position.getX(), (int) position.getY());

        // Clean up rendering
        renderer.endRendering();
    }

    // Unused methods
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
}
