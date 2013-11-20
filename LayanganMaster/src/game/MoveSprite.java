package game;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;


import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.modifier.MoveXModifier;
import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.augmentedreality.BaseAugmentedRealityGameActivity;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import org.andengine.util.HorizontalAlign;
import org.andengine.util.adt.io.in.IInputStreamOpener;
import org.andengine.util.debug.Debug;

import android.graphics.Typeface;
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
	private Camera camera;
	private Scene scene;
	
	// Target sprite 
	private ITexture mTextureCoin;
	private ITextureRegion mTargetTextureRegion;
	private LinkedList targetLL;
	private LinkedList TargetsToBeAdded;
	private int hitCount;
	// This one is for the font
	private BitmapTextureAtlas mFontTexture;
	private Font mFont;
	private Text score;

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
		camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

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

		scene = new Scene();
		scene.setBackground(new Background(0,0,0,0));
		
		VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		// settings score to the value of the max score to make sure it appears
		// correctly on the screen
		//score = new Text(camera.getWidth() - score.getWidth() - 5, 5, this.mFont, "0", new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
		
		
		//score = new Text(0, 0, mFont, String.valueOf(0));
		// repositioning the score later so we can use the score.getWidth()
		//score.setPosition(mCamera.getWidth() - score.getWidth() - 5, 5);

		/* Calculate the coordinates for the face, so its centered on the camera. */
		centerX = (CAMERA_WIDTH - this.mFaceTextureRegion.getWidth()) / 2;
		centerY = (CAMERA_HEIGHT - this.mFaceTextureRegion.getHeight()) / 2;

		/* Create the face and add it to the scene. */
		//scene.attachChild(score);
		this.face = new Sprite(centerX, centerY, this.mFaceTextureRegion, this.getVertexBufferObjectManager());
		scene.attachChild(this.face);
		this.targetLL = new LinkedList();
		this.TargetsToBeAdded = new LinkedList();
		createSpriteSpawnTimeHandler();
		
		/* The actual collision-checking. */
		scene.registerUpdateHandler(new IUpdateHandler() {
			@Override
			public void reset() { }

			@Override
			public void onUpdate(final float pSecondsElapsed) {
				Iterator<Sprite> targets = targetLL.iterator();
				Sprite _target;
				boolean hit = false;

				// iterating over the targets
				while (targets.hasNext()) {
					_target = targets.next();

				
					// if the targets collides with a projectile, remove the
					// projectile and set the hit flag to true
					if (_target.collidesWith(face)) {
						removeSprite(_target, targets);
						hit = true;
						break;
					}
					
					// if a projectile hit the target, remove the target, increment
					// the hit count, and update the score
					if (hit) {
						removeSprite(_target, targets);
						hit = false;
						hitCount++;
						//score.setText(String.valueOf(hitCount));
					}
				}

				targetLL.addAll(TargetsToBeAdded);
				TargetsToBeAdded.clear();
			}
		});
		
		return scene;
	}
	
	/* safely detach the sprite from the scene and remove it from the iterator */
	public void removeSprite(final Sprite _sprite, Iterator<Sprite> it) {
		runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				scene.detachChild(_sprite);
			}
		});
		it.remove();
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
			
			this.mTextureCoin = new BitmapTexture(this.getTextureManager(), new IInputStreamOpener() {
				@Override
				public InputStream open() throws IOException {
					return getAssets().open("gfx/coin.png");
				}
			});
			
			
			// preparing the font
			//this.mFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 32);
			//this.mFont.load();
			this.mTexture.load();
			this.mFaceTextureRegion = TextureRegionFactory.extractFromTexture(this.mTexture);
			this.mTextureCoin.load();
			this.mTargetTextureRegion = TextureRegionFactory.extractFromTexture(this.mTextureCoin);
			
		} catch (IOException e) {
			Debug.e(e);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================
	public void addTarget() {
	    Random rand = new Random();
	 
	    float y =  camera.getHeight() + mTargetTextureRegion.getHeight();
	    float minX = mTargetTextureRegion.getWidth();
	    float maxX =  camera.getWidth() - mTargetTextureRegion.getWidth();
	    float rangeX = maxX - minX;
	    float x = rand.nextFloat()*rangeX + minX;
	    
	    
	    		
	 
	    Sprite target = new Sprite(x, y, this.mTargetTextureRegion.deepCopy(), this.getVertexBufferObjectManager());
	    
	    this.scene.attachChild(target);
	 
	    int minDuration = 2;
	    int maxDuration = 4;
	    int rangeDuration = maxDuration - minDuration;
	    int actualDuration = rand.nextInt(rangeDuration) + minDuration;
	 
	    MoveYModifier mod = new MoveYModifier(actualDuration,-target.getHeight(),
	    		target.getY() );
	    target.registerEntityModifier(mod.deepCopy());
	    
	 
	    this.TargetsToBeAdded.add(target);
	 
	}
	
	private void createSpriteSpawnTimeHandler() {
	    TimerHandler spriteTimerHandler;
	    float mEffectSpawnDelay = 1f;
	 
	    spriteTimerHandler = new TimerHandler(mEffectSpawnDelay, true,
	    new ITimerCallback() {
	 
	        @Override
	        public void onTimePassed(TimerHandler pTimerHandler) {
	            addTarget();
	        }
	    });
	 
	    getEngine().registerUpdateHandler(spriteTimerHandler);
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}

