package game;

import java.io.IOException;
import java.io.InputStream;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.augmentedreality.BaseAugmentedRealityGameActivity;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;

import org.andengine.util.adt.io.in.IInputStreamOpener;
import org.andengine.util.debug.Debug;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga
 *
 * @author Nicolas Gramlich
 * @since 11:54:51 - 03.04.2010
 */
public class MoveSprite extends BaseAugmentedRealityGameActivity implements SensorEventListener{
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;

	// ===========================================================
	// Fields
	// ===========================================================

	private ITexture mTexture;
	private ITextureRegion mFaceTextureRegion;
	private Sprite face;
	private int accellerometerSpeedX;
	private int accellerometerSpeedY;
	private SensorManager sensorManager;
	private float centerX;
	private float centerY;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_SENSOR, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}


	
	public Scene onCreateScene() {
		sensorManager = (SensorManager) this
				.getSystemService(this.SENSOR_SERVICE);
		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				sensorManager.SENSOR_DELAY_GAME);
		
		this.mEngine.registerUpdateHandler(new FPSLogger());
		this.mEngine.registerUpdateHandler(new IUpdateHandler() {
			public void onUpdate(float pSecondsElapsed) {
				updateSpritePosition();
			}

			public void reset() {
				// TODO Auto-generated method stub
			}
		});

		final Scene scene = new Scene();
		scene.setBackground(new Background(0,0,0,0));

		/* Calculate the coordinates for the face, so its centered on the camera. */
		centerX = (CAMERA_WIDTH - this.mFaceTextureRegion.getWidth()) / 2;
		centerY = (CAMERA_HEIGHT - this.mFaceTextureRegion.getHeight()) / 3;

		/* Create the face and add it to the scene. */
		this.face = new Sprite(centerX, centerY, this.mFaceTextureRegion, this.getVertexBufferObjectManager());
		scene.attachChild(this.face);

		return scene;
	}

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				accellerometerSpeedX = (int) event.values[1];
				accellerometerSpeedY = (int) event.values[0];
				break;
			}
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//
	}
	
	private void updateSpritePosition() {
		if ((accellerometerSpeedX != 0) || (accellerometerSpeedY != 0)) {
			// Set the Boundary limits
			int tL = 0;
			int lL = 0;
			int rL = CAMERA_WIDTH - (int) face.getWidth();
			int bL = CAMERA_HEIGHT - (int) face.getHeight();

			// Calculate New X,Y Coordinates within Limits
			if (centerX >= lL)
				centerX += accellerometerSpeedX;
			else
				centerX = lL;
			if (centerX <= rL)
				centerX += accellerometerSpeedX;
			else
				centerX = rL;
			/*
			if (centerY >= tL)
				centerY += accellerometerSpeedY;
			else
				centerY = tL;
			if (centerY <= bL)
				centerY += accellerometerSpeedY;
			else
				centerY = bL;
			 */
			// Double Check That New X,Y Coordinates are within Limits
			if (centerX < lL)
				centerX = lL;
			else if (centerX > rL)
				centerX = rL;
			/*
			if (centerY < tL)
				centerY = tL;
			else if (centerY > bL)
				centerY = bL;
			*/
			face.setPosition(centerX, centerY);
		}
	}

	@Override
	protected void onCreateResources() {
		// TODO Auto-generated method stub
		try {
			this.mTexture = new BitmapTexture(this.getTextureManager(), new IInputStreamOpener() {
				@Override
				public InputStream open() throws IOException {
					return getAssets().open("gfx/kite2.png");
				}
			});

			this.mTexture.load();
			this.mFaceTextureRegion = TextureRegionFactory.extractFromTexture(this.mTexture);
		} catch (IOException e) {
			Debug.e(e);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}

