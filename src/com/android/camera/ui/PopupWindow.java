package com.android.camera.ui;

import android.graphics.Rect;
import android.view.View.MeasureSpec;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;

import javax.microedition.khronos.opengles.GL11;

public class PopupWindow extends GLView {

    protected Texture mAnchor;
    protected int mAnchorOffset;

    protected int mAnchorPosition;
    private final RotatePane mRotatePane = new RotatePane();

    protected NinePatchTexture mBackground;

    public PopupWindow() {
        super.addComponent(mRotatePane);
    }

    public void setBackground(NinePatchTexture background) {
        if (background == mBackground) return;
        mBackground = background;
        if (background != null) {
            setPaddings(mBackground.getPaddings());
        } else {
            setPaddings(0, 0, 0, 0);
        }
        invalidate();
    }

    public void setAnchor(Texture anchor, int offset) {
        mAnchor = anchor;
        mAnchorOffset = offset;
    }

    @Override
    public void addComponent(GLView component) {
        throw new UnsupportedOperationException("use setContent(GLView)");
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int widthMode = MeasureSpec.getMode(widthSpec);
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            Rect p = mPaddings;
            int width = MeasureSpec.getSize(widthSpec);
            widthSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(0, width - p.left - p.right
                    - mAnchor.getWidth() + mAnchorOffset), widthMode);
        }

        int heightMode = MeasureSpec.getMode(heightSpec);
        if (heightMode != MeasureSpec.UNSPECIFIED) {
            int height = MeasureSpec.getSize(widthSpec);
            widthSpec = MeasureSpec.makeMeasureSpec(Math.max(
                    0, height - mPaddings.top - mPaddings.bottom), heightMode);
        }

        int cWidth = 0;
        int cHeight = 0;

        Rect p = mPaddings;
        GLView child = mRotatePane;
        child.measure(widthSpec, heightSpec);
        setMeasuredSize(child.getMeasuredWidth()
                + p.left + p.right + mAnchor.getWidth() - mAnchorOffset,
                child.getMeasuredHeight() + p.top + p.bottom);
    }

    @Override
    protected void onLayout(
            boolean change, int left, int top, int right, int bottom) {
        Rect p = getPaddings();
        GLView view = mRotatePane;
        view.layout(p.left, p.top,
                getWidth() - p.right - mAnchor.getWidth() + mAnchorOffset,
                getHeight() - p.bottom);
    }

    public void setAnchorPosition(int yoffset) {
        mAnchorPosition = yoffset;
    }

    @Override
    protected void renderBackground(GLRootView rootView, GL11 gl) {
        int width = getWidth();
        int height = getHeight();
        int aWidth = mAnchor.getWidth();
        int aHeight = mAnchor.getHeight();

        Rect p = mPaddings;
        int aXoffset = width - aWidth;
        int aYoffset = Math.max(p.top, mAnchorPosition - aHeight / 2);
        aYoffset = Math.min(aYoffset, height - p.bottom - aHeight);

        if (mAnchor != null) {
            if (mAnchor.bind(rootView, gl)) {
                rootView.draw2D(aXoffset, aYoffset, aWidth, aHeight);
            }
        }

        Texture backup = null;
        try {
            backup = rootView.copyTexture2D(
                    aXoffset, aYoffset, aWidth, aHeight);
        } catch (GLOutOfMemoryException e) {
            e.printStackTrace();
        }

        if (mBackground != null) {
            mBackground.setSize(width - aWidth + mAnchorOffset, height);
            if (mBackground.bind(rootView, gl)) {
                rootView.draw2D(
                        0, 0, mBackground.getWidth(), mBackground.getHeight());
            }
        }

        if (backup.bind(rootView, gl)) {
            gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
            rootView.draw2D(aXoffset, aYoffset, aWidth, aHeight, 1);
            gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    public void setContent(GLView content) {
        mRotatePane.setContent(content);
    }

    @Override
    public void clearComponents() {
        throw new UnsupportedOperationException();
    }

    public void popup() {
        setVisibility(GLView.VISIBLE);

        AnimationSet set = new AnimationSet(false);
        Animation scale = new ScaleAnimation(
                0.7f, 1f, 0.7f, 1f, getWidth(), getHeight() / 2);
        Animation alpha = new AlphaAnimation(0.5f, 1.0f);

        set.addAnimation(scale);
        set.addAnimation(alpha);
        scale.setDuration(200);
        alpha.setDuration(200);
        scale.setInterpolator(new OvershootInterpolator());
        startAnimation(set);
    }

    public void popoff() {
        setVisibility(GLView.INVISIBLE);
        Animation alpha = new AlphaAnimation(0.7f, 0.0f);
        alpha.setDuration(100);
        startAnimation(alpha);
    }

    public void setOrientation(int orientation) {
        switch (orientation) {
            case 90:
                mRotatePane.setOrientation(RotatePane.LEFT);
                break;
            case 180:
                mRotatePane.setOrientation(RotatePane.DOWN);
                break;
            case 270:
                mRotatePane.setOrientation(RotatePane.RIGHT);
                break;
            default:
                mRotatePane.setOrientation(RotatePane.UP);
                break;
        }
    }

}
