package com.example.sensorsassignment;

import android.content.Context;
import android.renderscript.Matrix4f;
import android.view.MotionEvent;

import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;

public class Renderer extends RajawaliRenderer {

    public Context context;
    private DirectionalLight directionalLight;
    private Sphere earthSphere;
    private Cube cube;

    public float[] angles = {0,0,0};
    public Renderer(Context context) {
        super(context);
        this.context = context;
        setFrameRate(20);
    }

    public void onTouchEvent(MotionEvent event){

    }

    public void onOffsetsChanged(float x, float y, float z, float w, int i, int j){
    }


    public void initScene(){

        directionalLight = new DirectionalLight(1f, .2f, -1.0f);
        directionalLight.setColor(1.0f, 1.0f, 1.0f);
        directionalLight.setPower(2);
        getCurrentScene().addLight(directionalLight);
        Material material = new Material();
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(0);

        Texture earthTexture = new Texture("Earth", R.drawable.earth);

        try {
            material.addTexture(earthTexture);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }


        earthSphere = new Sphere(1, 24, 24);
        earthSphere.setMaterial(material);

        getCurrentScene().addChild(earthSphere);

        getCurrentCamera().setZ(5f);


    }

    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        Quaternion quaternion = new Quaternion();
        earthSphere.setOrientation(quaternion.fromEuler(360 - angles[1], 360 -angles[0], 360 -angles[2]));

    }


}