package lvl2advanced.p01run;

import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import oglutils.OGLBuffers;
import oglutils.OGLTexture2D;
import oglutils.OGLUtils;
import oglutils.ShaderUtils;
import oglutils.ToFloatArray;

import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;

import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4PerspRH;
import transforms.Vec3D;

/**
 * GLSL sample:<br/>
 * Load and apply cube texture using OGLTexture from oglutils package<br/>
 * Requires JOGL 2.3.0 or newer
 * 
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2016-10-06
 */

public class RendererNoAwt implements GLEventListener, MouseListener,
	KeyListener {

	int width, height, ox, oy;

	OGLBuffers buffers;
	//do not use textRenderer in no AWT demo
	//OGLTextRenderer textRenderer = new OGLTextRenderer();

	int shaderProgram, locMat;
	
	OGLTexture2D texture;

	Camera cam = new Camera();
	Mat4 proj;
	OGLTexture2D.Viewer textureViewer;
	
	@Override
	public void init(GLAutoDrawable glDrawable) {
		// check whether shaders are supported
		OGLUtils.shaderCheck(glDrawable.getGL().getGL2GL3());
		
		glDrawable.setGL(OGLUtils.getDebugGL(glDrawable.getGL()));
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();

		// jogl parameters
		OGLUtils.printJOGLparameters();
		System.out.println();

		// java parameters
		OGLUtils.printJAVAparameters();
		System.out.println();

		// opengl parameters
		OGLUtils.printOGLparameters(gl);
		System.out.println();

		// shader files are in /shaders/ directory
		// shaders directory must be set as a source directory of the project
		// e.g. in Eclipse via main menu Project/Properties/Java Build Path/Source
		shaderProgram = ShaderUtils.loadProgram(gl, "/lvl2advanced/p01run/texture");
		createBuffers(gl);

		locMat = gl.glGetUniformLocation(shaderProgram, "mat");

		texture = new OGLTexture2D(gl, "/textures/mosaic.jpg");

		cam = cam.withPosition(new Vec3D(5, 5, 2.5)).withAzimuth(Math.PI * 1.25).withZenith(Math.PI * -0.125);

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);
		textureViewer = new OGLTexture2D.Viewer(gl);
	}

	void createBuffers(GL2GL3 gl) {
				// vertices are not shared among triangles (and thus faces) so each face
				// can have a correct normal in all vertices
				// also because of this, the vertices can be directly drawn as GL_TRIANGLES
				// (three and three vertices form one face) 
				// triangles defined in index buffer
		float[] cube = {
								// bottom (z-) face
								1, 0, 0,	0, 0, -1, 	1, 0,
								0, 0, 0,	0, 0, -1,	0, 0, 
								1, 1, 0,	0, 0, -1,	1, 1, 
								0, 1, 0,	0, 0, -1,	0, 1, 
								// top (z+) face
								1, 0, 1,	0, 0, 1,	1, 0, 
								0, 0, 1,	0, 0, 1,	0, 0, 
								1, 1, 1,	0, 0, 1,	1, 1,
								0, 1, 1,	0, 0, 1,	0, 1,
								// x+ face
								1, 1, 0,	1, 0, 0,	1, 0,
								1, 0, 0,	1, 0, 0,	0, 0, 
								1, 1, 1,	1, 0, 0,	1, 1,
								1, 0, 1,	1, 0, 0,	0, 1,
								// x- face
								0, 1, 0,	-1, 0, 0,	1, 0,
								0, 0, 0,	-1, 0, 0,	0, 0, 
								0, 1, 1,	-1, 0, 0,	1, 1,
								0, 0, 1,	-1, 0, 0,	0, 1,
								// y+ face
								1, 1, 0,	0, 1, 0,	1, 0,
								0, 1, 0,	0, 1, 0,	0, 0, 
								1, 1, 1,	0, 1, 0,	1, 1,
								0, 1, 1,	0, 1, 0,	0, 1,
								// y- face
								1, 0, 0,	0, -1, 0,	1, 0,
								0, 0, 0,	0, -1, 0,	0, 0, 
								1, 0, 1,	0, -1, 0,	1, 1,
								0, 0, 1,	0, -1, 0,	0, 1
						};

		int[] indexBufferData = new int[36];
		for (int i = 0; i < 6; i++) {
			indexBufferData[i * 6] = i * 4;
			indexBufferData[i * 6 + 1] = i * 4 + 1;
			indexBufferData[i * 6 + 2] = i * 4 + 2;
			indexBufferData[i * 6 + 3] = i * 4 + 1;
			indexBufferData[i * 6 + 4] = i * 4 + 2;
			indexBufferData[i * 6 + 5] = i * 4 + 3;
		}

		OGLBuffers.Attrib[] attributes = { 
				new OGLBuffers.Attrib("inPosition", 3), 
				new OGLBuffers.Attrib("inNormal", 3),
				new OGLBuffers.Attrib("inTextureCoordinates", 2) };

		buffers = new OGLBuffers(gl, cube, attributes, indexBufferData);
	}
	
	@Override
	public void display(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		
		gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(shaderProgram); 
		gl.glUniformMatrix4fv(locMat, 1, false,
				ToFloatArray.convert(cam.getViewMatrix().mul(proj)), 0);
		
		texture.bind(shaderProgram, "textureID", 0);
		
		buffers.draw(GL2GL3.GL_TRIANGLES, shaderProgram);
		
		textureViewer.view(texture, -1, -1, 0.5);
		
		//String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");
		
		//textRenderer.drawStr2D(3, height-20, text);
		//textRenderer.drawStr2D(width-90, 3, " (c) PGRF UHK");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		this.width = width;
		this.height = height;
		proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
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
	public void mouseDragged(MouseEvent e) {
		cam = cam.addAzimuth((double) Math.PI * (ox - e.getX()) / width)
				.addZenith((double) Math.PI * (e.getY() - oy) / width);
		ox = e.getX();
		oy = e.getY();
	}

	@Override
	public void mouseWheelMoved(MouseEvent e) {
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
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		GL2GL3 gl = glDrawable.getGL().getGL2GL3();
		gl.glDeleteProgram(shaderProgram);
	}
}