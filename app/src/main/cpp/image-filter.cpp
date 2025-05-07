#include <jni.h>
#include <android/bitmap.h>
#include <string>
#include <cstdint>

extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_rutubishi_imagefilterktcpp_MainActivity_jniWelcomeMessage(
            JNIEnv* env,
            jobject
    ){
        std::string hello = "Hello Compose From C++";
        return env -> NewStringUTF(hello.c_str());
    }

JNIEXPORT void JNICALL
Java_com_rutubishi_imagefilterktcpp_MainActivity_processToGrayscale(
        JNIEnv* env,
        jobject,
        jobject bitmap
        ){

        AndroidBitmapInfo info;
        void* pixels;

        if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return;
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return;

        uint32_t* line = (uint32_t*)pixels;
        for (int y = 0; y < info.height; ++y) {
            for (int x = 0; x < info.width; ++x) {
                uint32_t* pixel = line + x;

                uint8_t a = (*pixel >> 24) & 0xFF;
                uint8_t r = (*pixel >> 16) & 0xFF;
                uint8_t g = (*pixel >> 8) & 0xFF;
                uint8_t b = (*pixel) & 0xFF;

                uint8_t gray = (uint8_t)(0.3 * r + 0.59 * g + 0.11 * b);

                *pixel = (a << 24) | (gray << 16) | (gray << 8) | gray;
            }
        line = (uint32_t*)((char*)line + info.stride);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }

JNIEXPORT jobject JNICALL
Java_com_rutubishi_imagefilterktcpp_MainActivity_processToGrayscaleOptimal(
        JNIEnv *env,
        jobject,
        jobject originalBitmap
) {
    AndroidBitmapInfo info;
    void* pixels;

    if (AndroidBitmap_getInfo(env, originalBitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) return nullptr;
    if (AndroidBitmap_lockPixels(env, originalBitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) return nullptr;

    int width = info.width;
    int height = info.height;
    int stride = info.stride;

    // Prepare to create new bitmap
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888FieldID = env->GetStaticFieldID(bitmapConfigClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888Obj = env->GetStaticObjectField(bitmapConfigClass, argb8888FieldID);

    jobject grayBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, width, height, argb8888Obj);

    if (grayBitmap == nullptr) {
        AndroidBitmap_unlockPixels(env, originalBitmap);
        return nullptr;
    }

    void* grayPixels;
    if (AndroidBitmap_lockPixels(env, grayBitmap, &grayPixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        AndroidBitmap_unlockPixels(env, originalBitmap);
        return nullptr;
    }

    uint8_t* srcLine = reinterpret_cast<uint8_t*>(pixels);
    uint8_t* dstLine = reinterpret_cast<uint8_t*>(grayPixels);

    for (int y = 0; y < height; ++y) {
        uint32_t* srcPixel = reinterpret_cast<uint32_t*>(srcLine);
        uint32_t* dstPixel = reinterpret_cast<uint32_t*>(dstLine);

        for (int x = 0; x < width; ++x) {
            uint32_t pixel = srcPixel[x];

            uint8_t a = (pixel >> 24) & 0xFF;
            uint8_t r = (pixel >> 16) & 0xFF;
            uint8_t g = (pixel >> 8) & 0xFF;
            uint8_t b = (pixel) & 0xFF;

            // Approximate grayscale formula: (r*30 + g*59 + b*11)/100
            // Faster integer math instead of floating point
            uint8_t gray = (uint8_t)((r * 30 + g * 59 + b * 11) / 100);

            dstPixel[x] = (a << 24) | (gray << 16) | (gray << 8) | gray;
        }

        srcLine += stride;
        dstLine += stride;
    }

    AndroidBitmap_unlockPixels(env, originalBitmap);
    AndroidBitmap_unlockPixels(env, grayBitmap);

    return grayBitmap;
}

}




