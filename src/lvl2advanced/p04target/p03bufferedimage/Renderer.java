package lvl2advanced.p04target.p03bufferedimage;


import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLRenderTarget;
import oglutils.OGLTexImageFloat;
import oglutils.OGLTextRenderer;
import oglutils.OGLTexture2D;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;
import oglutils.ToFloatArray;

import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4PerspRH;
import transforms.Mat4Scale;
import transforms.Vec3D;

/**
 * Ukazka pro praci s shadery v GLSL:
 * vytvoreni renderTargetu pro ulozeni barvy pristup pres BufferedImage a Graphics
 * upraveno pro JOGL 2.3.0 a vyssi
 *
 * @author PGRF FIM UHK
 * @version 2.0
 * @since   2015-11-24
 */

public class Renderer implements GLEventListener, MouseListener,
		MouseMotionListener, KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers;
	//OGLTextRenderer textRenderer;

	int shaderProgram, locMat;

	OGLTexture2D texture, textureColor;

	Camera cam = new Camera();
	Mat4 proj;

	OGLRenderTarget renderTarget;
	boolean draw = false;
	OGLTexture2D texRGB;
	OGLTexture2D.Viewer textureViewer;

	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		OGLUtils.shaderCheck(glDrawable.getGL().getGL2GL3());

		//glDrawable.setGL(OGLUtils.getDebugGL(glDrawable.getGL()));
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		OGLUtils.printOGLparameters(gl);

		//textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());

		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p04target/p03bufferedimage/texture");

		createBuffers(gl);

		locMat = gl.glGetUniformLocation(shaderProgram, "mat");

		texture = new OGLTexture2D(gl, "/textures/bricks.jpg");

		renderTarget = new OGLRenderTarget(gl, 500, 500);

		cam = cam.withPosition(new Vec3D(5, 5, 2.5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.125);

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTexture2D.Viewer(gl);
	}

	void createBuffers(GL2GL3 gl) {
		float[] cube = {
				// bottom (z-) face
				1, 0, 0,	0, 0, -1,
				0, 0, 0,	0, 0, -1,
				1, 1, 0,	0, 0, -1,
				0, 1, 0,	0, 0, -1,
				// top (z+) face
				1, 0, 1,	0, 0, 1,
				0, 0, 1,	0, 0, 1,
				1, 1, 1,	0, 0, 1,
				0, 1, 1,	0, 0, 1,
				// x+ face
				1, 1, 0,	1, 0, 0,
				1, 0, 0,	1, 0, 0,
				1, 1, 1,	1, 0, 0,
				1, 0, 1,	1, 0, 0,
				// x- face
				0, 1, 0,	-1, 0, 0,
				0, 0, 0,	-1, 0, 0,
				0, 1, 1,	-1, 0, 0,
				0, 0, 1,	-1, 0, 0,
				// y+ face
				1, 1, 0,	0, 1, 0,
				0, 1, 0,	0, 1, 0,
				1, 1, 1,	0, 1, 0,
				0, 1, 1,	0, 1, 0,
				// y- face
				1, 0, 0,	0, -1, 0,
				0, 0, 0,	0, -1, 0,
				1, 0, 1,	0, -1, 0,
				0, 0, 1,	0, -1, 0
		};

		int[] indexBufferData = new int[36];
		for (int i = 0; i<6; i++){
			indexBufferData[i*6] = i*4;
			indexBufferData[i*6 + 1] = i*4 + 1;
			indexBufferData[i*6 + 2] = i*4 + 2;
			indexBufferData[i*6 + 3] = i*4 + 1;
			indexBufferData[i*6 + 4] = i*4 + 2;
			indexBufferData[i*6 + 5] = i*4 + 3;
		}
		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 3),
				new OGLBuffers.Attrib("inNormal", 3)
		};

		buffers = new OGLBuffers(gl, cube, attributes, indexBufferData);

	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		gl.glUseProgram(shaderProgram);

		// nastavime nas render target
		renderTarget.bind();

		gl.glClearColor(0.1f, 0.2f, 0.3f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		texture.bind(shaderProgram, "textureID", 0);

		gl.glUniformMatrix4fv(locMat, 1, false,
				ToFloatArray.convert(cam.getViewMatrix().mul(proj)
						.mul(new Mat4Scale((double) width / height, 1, 1))), 0);
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		// ziskani kontextu textury
		textureColor = renderTarget.getColorTexture();

		if (!draw) {
			draw = true;

			// prace s texturou jako BufferedImage
			// ziskani dat textury jako pole 4xfloat
			OGLTexImageFloat imgFloat = textureColor.getTexImage(new OGLTexImageFloat.Format(4));

			// vytvoreni klasicke textury
			texRGB = new OGLTexture2D(gl, imgFloat.toOGLTexImageByte());
			// prevod na BufferedImage
			BufferedImage img = texRGB.toBufferedImage();
			// vykresleni
			Graphics graph = img.getGraphics();
			Graphics2D gr = (Graphics2D) graph;
			gr.setColor(new Color(0xffff));
			gr.fillOval(0, 0, width / 10, height / 10);
			gr.setColor(new Color(0xff00ff));
			gr.drawLine(width / 2, 0, 0, height / 2);
			gr.setColor(new Color(0xffffff));
			gr.drawRect(100, 100, 200, 200);


			gr.setFont(new Font( "arial", Font.BOLD, 80));
			if (System.getProperty("os.name").startsWith("Mac")) {
				AffineTransform at = new AffineTransform();
				at.rotate(-Math.PI /2, texRGB.getWidth()/2, texRGB.getHeight()/2);
				//at.scale(-texRGB.getWidth()/2, texRGB.getHeight()/2);
				at.translate(texRGB.getWidth()/2, 0);
				at.scale(-1, 1);
				at.translate(-texRGB.getWidth()/2, 0);

				at.rotate(Math.PI /2, texRGB.getWidth()/2, texRGB.getHeight()/2);
				//gr.rotate(Math.PI / 2, texRGB.getWidth()/2, texRGB.getHeight()/2);
				gr.setTransform(at);
				gr.drawString("text", 200, 200);

			} else {
				gr.drawString("text", texRGB.getWidth()/2, texRGB.getHeight()/2);
			}


			// ulozeni dat do textury
			texRGB.fromBufferedImage(img);

		}

		// nastavime vychozi render target
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		// vysledek predchoziho kresleni pouzijeme jako texturu, vice moznosti
		//renderTarget.bindColorTexture(shaderProgram, "textureID", 0);
		//renderTarget.getColorTexture().bind(shaderProgram, "textureID", 0);
		texRGB.bind(shaderProgram, "textureID", 0);
		//renderTarget.bindDepthTexture(shaderProgram, "textureID", 0);


		gl.glUniformMatrix4fv(locMat, 1, false,
				ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);

		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		gl.glUseProgram(0); // bez shaderu

		textureViewer.view(texRGB, -1, -1, 1.5, height / (double) width);

		String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");

		//textRenderer.drawStr2D(3, height-20, text);
		//textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
						int height) {
		this.width = width;
		this.height = height;
		proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 1.0, 100.0);
		System.out.println(width);
		//textRenderer.updateSize(width, height);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		ox = e.getX();
		oy = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		cam = cam.addAzimuth((double) Math.PI * (ox - e.getX()) / width)
				.addZenith((double) Math.PI * (e.getY() - oy) / width);
		ox = e.getX();
		oy = e.getY();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_W:
				cam = cam.forward(1);
				break;
			case KeyEvent.VK_D:
				cam = cam.right(1);
				break;
			case KeyEvent.VK_S:
				cam = cam.backward(1);
				break;
			case KeyEvent.VK_A:
				cam = cam.left(1);
				break;
			case KeyEvent.VK_CONTROL:
				cam = cam.down(1);
				break;
			case KeyEvent.VK_SHIFT:
				cam = cam.up(1);
				break;
			case KeyEvent.VK_SPACE:
				cam = cam.withFirstPerson(!cam.getFirstPerson());
				break;
			case KeyEvent.VK_R:
				cam = cam.mulRadius(0.9f);
				break;
			case KeyEvent.VK_F:
				cam = cam.mulRadius(1.1f);
				break;
			case KeyEvent.VK_N:
				draw = false;
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		gl.glDeleteProgram(shaderProgram);
	}

}