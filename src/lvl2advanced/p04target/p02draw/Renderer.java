package lvl2advanced.p04target.p02draw;


import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLRenderTarget;
import oglutils.OGLTexImageFloat;
import oglutils.OGLTextRenderer;
import oglutils.OGLTexture;
import oglutils.OGLTexture2D;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;
import oglutils.ToFloatArray;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4PerspRH;
import transforms.Mat4Scale;
import transforms.Vec3D;

/**
* Ukazka pro praci s shadery v GLSL:
* vytvoreni renderTargetu pro ulozeni barvy i hloubky, modifikace obsahu, zobrazeni
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

	OGLTexture2D texture, textureColor, textureDepth;
	
	Camera cam = new Camera();
	Mat4 proj;

	OGLRenderTarget renderTarget;
	OGLTexture.Viewer textureViewer;
	boolean modificate = true;
	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		OGLUtils.shaderCheck(glDrawable.getGL().getGL2GL3());
		
		//glDrawable.setGL(OGLUtils.getDebugGL(glDrawable.getGL()));
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		OGLUtils.printOGLparameters(gl);

		//textRenderer = new OGLTextRenderer(gl, glDrawable.getSurfaceWidth(), glDrawable.getSurfaceHeight());
		
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p04target/p02draw/texture");

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

		if (modificate) {
			// ziskani kontextu textury
			textureColor = renderTarget.getColorTexture();
			textureDepth = renderTarget.getDepthTexture();

			// ziskani barevnych dat textury jako pole RGBA 4xfloat
			OGLTexImageFloat imgFloat = textureColor.getTexImage(new OGLTexImageFloat.Format(4));

			// pristup k datum v texture jako 4xfloat, pozor pristup je po
			// slozkach
			imgFloat.setPixel(100, 125, 0, 1.0f); // red
			imgFloat.setPixel(100, 125, 1, 0.0f); // green
			imgFloat.setPixel(100, 125, 2, 1.0f); // blue
			imgFloat.setPixel(100, 125, 3, 1.0f); // alpha
			for (int i = 0; i < 200; i++) {
				imgFloat.setPixel(i, i, 0, imgFloat.getPixel(100, 125, 0));
				imgFloat.setPixel(i, i, 1, imgFloat.getPixel(100, 125, 1));
				imgFloat.setPixel(i, i, 2, imgFloat.getPixel(100, 125, 2));
				imgFloat.setPixel(i, i, 3, imgFloat.getPixel(100, 125, 3));
			}
			for (int i = 0; i < 200; i++) {
				imgFloat.setPixel(i + 10, i, 0, i / 200f);
				imgFloat.setPixel(i + 10, i, 1, 1f);
				imgFloat.setPixel(i + 10, i, 2, i / 200f);
				imgFloat.setPixel(i + 10, i, 3, 1f);
			}
			// ulozeni dat do textury
			textureColor.setTexImage(imgFloat);

			// ziskani hloubkovych dat textury jako pole 1xfloat
			imgFloat = textureDepth.getTexImage(new OGLTexImageFloat.FormatDepth());
			for (int i = 0; i < 200; i++)
				imgFloat.setPixel(i + 10, i, i / 200.0f);
			textureDepth.setTexImage(imgFloat);
		}
	
		// nastavime vychozi render target
		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);

		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

		//vysledek predchoziho kresleni pouzijeme jako texturu, vice moznosti
		//renderTarget.bindColorTexture(shaderProgram, "textureID", 0);
		renderTarget.getColorTexture().bind(shaderProgram, "textureID", 0);
		//renderTarget.bindDepthTexture(shaderProgram, "textureID", 0);

		
		gl.glUniformMatrix4fv(locMat, 1, false,
				ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);

		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);

		gl.glUseProgram(0); // bez shaderu
		textureViewer.view(textureColor, -1, 0, 0.5, height / (double) width);
		textureViewer.view(textureDepth, -1, -1, 0.5, height / (double) width);
		
		String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD, [m] draw");
		
		//textRenderer.drawStr2D(3, height-20, text);
		//textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
		proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 1.0, 100.0);
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
		// System.out.print(cam.getViewMatrix().string());
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
		case KeyEvent.VK_M:
			modificate = !modificate;;
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