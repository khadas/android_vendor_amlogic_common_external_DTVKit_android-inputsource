/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#define LOG_TAG "dtvkit-jni"

//#include <binder/ProcessState.h>
//#include <binder/IServiceManager.h>
//#include <android_runtime/android_view_Surface.h>
//#include <android/native_window.h>
////#include <gui/Surface.h>
//#include <gui/IGraphicBufferProducer.h>
//#include <ui/GraphicBuffer.h>
//#include <gralloc_usage_ext.h>
//#include <hardware/gralloc1.h>
//#include "amlogic/am_gralloc_ext.h"
#include <dlfcn.h>
#include <android/hidl/memory/1.0/IMemory.h>
#include <hidlmemory/mapping.h>
#include "org_droidlogic_dtvkit_DtvkitGlueClient.h"

using namespace android;
using ::android::hidl::memory::V1_0::IMemory;
using ::android::sp;
static JavaVM   *gJavaVM = NULL;
sp<DTVKitClientJni> mpDtvkitJni;
static jmethodID notifySubtitleCallback;
static jmethodID notifyDvbCallback;
static jobject DtvkitObject;
//sp<Surface> mSurface;
//sp<NativeHandle> mSourceHandle;

static jmethodID notifySubtitleCallbackEx;
static jmethodID notifySubtitleCbCtlEx;
static jmethodID notifyCCSubtitleCallbackEx;
static jmethodID notifyMixVideoEventCallback;

sp<amlogic::SubtitleServerClient> mSubContext;
static jboolean g_bSubStatus = false;

#define DTVKIT_SUBTITLE_ADD_OFFSET 4
#define SUBTITLE_DEMUX_SOURCE 4
#define SUBTITLE_SCTE27_SOURCE 1

#define SUBTITLE_SUB_TYPE_DVB    1
#define SUBTITLE_SUB_TYPE_TTXSUB 2
#define SUBTITLE_SUB_TYPE_SCTE   3
#define SUBTITLE_SUB_TYPE_TTX    4

#define START_SUB_TYOE_CLOSED_CAPTION -5
#define CALLBACK_SUB_TYPE_CLOSED_CAPTION 10
#define TT_EVENT_INDEXPAGE 14
#define TT_EVENT_GO_TO_PAGE 30
#define TT_EVENT_GO_TO_SUBTITLE 31
#define TT_EVENT_SET_REGION_ID 32


static void postSubtitleDataEx(int type, int width, int height, int dst_x, int dst_y, int dst_width, int dst_height, const char *data);
static void clearSubtitleDataEx();
static void setAFDToDVBCore(int dec_id, uint8_t afd);
static void postMixVideoEvent(int event);
static void setSubtitleOn(int pid, int type, int magazine, int page, int demuxId);
static void setSubtitleOff();
static void setSubtitlePause();
static void setSubtitleResume();
static void notitySubtitleTeletextEvent(int eventType);
static void subtitleTune(int type, int param1, int param2, int param3);

void SubtitleDataListenerImpl::onSubtitleEvent(const char *data, int size, int parserType,
            int x, int y, int width, int height,
            int videoWidth, int videoHeight, int cmd) {
    if (cmd) {
        postSubtitleDataEx(parserType, width, height, x, y, videoWidth, videoHeight, data);
    } else {
        clearSubtitleDataEx();
    }
}

void SubtitleDataListenerImpl::onSubtitleAfdEvent(int dec_id, int afd) {
    setAFDToDVBCore(dec_id, afd);

}

void SubtitleDataListenerImpl::onMixVideoEvent(int val) {
    postMixVideoEvent(val);
}

void SubtitleDataListenerImpl::onServerDied() {
    ALOGE("subtitle client server died");
}

static JNIEnv* getJniEnv(bool *needDetach) {
    int ret = -1;
    JNIEnv *env = NULL;
    ret = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);
    if (ret < 0) {
        ret = gJavaVM->AttachCurrentThread(&env, NULL);
        if (ret < 0) {
            ALOGE("Can't attach thread ret = %d", ret);
            return NULL;
        }
        *needDetach = true;
    }
    return env;
}

static void DetachJniEnv() {
    int result = gJavaVM->DetachCurrentThread();
    if (result != JNI_OK) {
        ALOGE("thread detach failed: %#x", result);
    }
}


static void setSubtitleStatus(bool bOn) {
    g_bSubStatus = bOn;
}

static jboolean getSubtitleStatus() {
    return g_bSubStatus;
}

static void postSubtitleData(int width, int height, int dst_x, int dst_y, int dst_width, int dst_height, uint8_t* data)
{
    ALOGD("callback sendSubtitleData data = %p", data);
    bool attached = false;
    JNIEnv *env = getJniEnv(&attached);

    if (env != NULL) {
        if (width != 0 || height != 0) {
            //ScopedLocalRef<jbyteArray> array (env, env->NewByteArray(width * height * 4));
            jintArray array = env->NewIntArray(width * height);
            env->SetIntArrayRegion(array, 0, width * height, (jint*)data);
            env->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, array);
            env->DeleteLocalRef(array);
        } else {
            env->CallVoidMethod(DtvkitObject, notifySubtitleCallback, width, height, dst_x, dst_y, dst_width, dst_height, NULL);
        }
    }
    if (attached) {
        DetachJniEnv();
    }
}

static void postSubtitleDataEx(int type, int width, int height, int dst_x, int dst_y, int dst_width, int dst_height,const char *data)
{
    //ALOGD("callback sendSubtitleData data = %p", data);
    bool attached = false;
    JNIEnv *env = getJniEnv(&attached);
    bool bSubOn = getSubtitleStatus();
    if (!bSubOn) return;

    if (env != NULL) {
        if (type == CALLBACK_SUB_TYPE_CLOSED_CAPTION) {
            jstring jccData = env->NewStringUTF((char *)data);
            env->CallVoidMethod(DtvkitObject, notifyCCSubtitleCallbackEx, true, jccData);
            env->DeleteLocalRef(jccData);
        } else {
            if (width != 0 || height != 0) {
                //ScopedLocalRef<jintArray> array (env, env->NewIntArray(width * height));
                jintArray array = env->NewIntArray(width * height);
                env->SetIntArrayRegion(array, 0, width * height, (jint*)data);
                env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, type, 0, 0, dst_x, dst_y, 9999, 0, NULL);
                env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, type, width, height, dst_x, dst_y,
                   dst_width, dst_height, array);
                env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, type, 0, 0, dst_x, dst_y, 0, 0, NULL);
                env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, type, 0, 0, dst_x, dst_y, 0, 9999, NULL);
            } else {
                env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, type, width, height, dst_x, dst_y,
                   dst_width, dst_height, NULL);
                }
        }
    }
    if (attached) {
        DetachJniEnv();
    }
}

static void clearSubtitleDataEx()
{
    //ALOGD("callback sendSubtitleData data = %p", data);
    bool attached = false;
    JNIEnv *env = getJniEnv(&attached);

    if (env != NULL) {
        env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, 0, 0, 0, 0, 0, 9999, 0, NULL);
        env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, 0, 0, 0, 0, 0, 0, 9999, NULL);
    }
    if (attached) {
        DetachJniEnv();
    }
}

static void clearCCSubtitleData()
{
    bool attached = false;
    JNIEnv *env = getJniEnv(&attached);
    if (env != NULL) {
        env->CallVoidMethod(DtvkitObject, notifyCCSubtitleCallbackEx, false, NULL);
    }

    if (attached) {
        DetachJniEnv();
    }

}

static void setAFDToDVBCore(int dec_id, uint8_t afd) {
    ALOGV("AFD value = 0x%xdfrom subtitleserver", afd);
    if (mpDtvkitJni != NULL)
        mpDtvkitJni->setAfd(dec_id, afd);
}

static void postMixVideoEvent(int event) {
    bool attached = false;
    JNIEnv *env = getJniEnv(&attached);

    if (env != NULL) {
        env->CallVoidMethod(DtvkitObject, notifyMixVideoEventCallback, event);
    }
    if (attached) {
        DetachJniEnv();
    }

}

static void postDvbParam(const std::string& resource, const std::string json, int id) {
    ALOGD("-callback postDvbParam resource:%s (%d), json:%s", resource.c_str(), id, json.c_str());
    bool attached = false;
    JNIEnv *env = getJniEnv(&attached);

    if (env != NULL) {
        //ALOGD("-callback event get ok");
        //ScopedLocalRef<jstring> jresource((env), (env)->NewStringUTF(resource.c_str()));
        //ScopedLocalRef<jstring> jjson((env),  (env)->NewStringUTF(json.c_str()));
        jstring jresource = env->NewStringUTF(resource.c_str());
        jstring jjson     = env->NewStringUTF(json.c_str());
        env->CallVoidMethod(DtvkitObject, notifyDvbCallback, jresource, jjson, id);
        env->DeleteLocalRef(jresource);
        env->DeleteLocalRef(jjson);
    }
    if (attached) {
        DetachJniEnv();
    }

}
pthread_once_t once = PTHREAD_ONCE_INIT;
void  DTVKitClientJni::once_run(void)
{
    if (NULL == mInstance)
         mInstance = new DTVKitClientJni();
}

DTVKitClientJni::DTVKitClientJni()  {
    mDkSession = DTVKitHidlClient::connect(DTVKitHidlClient::CONNECT_TYPE_HAL);
    mDkSession->setListener(this);
}

DTVKitClientJni *DTVKitClientJni::mInstance = NULL;
DTVKitClientJni *DTVKitClientJni::GetInstance() {
    pthread_once(&once, once_run);
    return mInstance;
}

DTVKitClientJni::~DTVKitClientJni()  {

}

std::string DTVKitClientJni::request(const std::string& resource, const std::string& json) {
    return mDkSession->request(resource, json);
}

void DTVKitClientJni::setAfd(int player, int afd) {
    mDkSession->setAfd(player, afd);
}

void DTVKitClientJni::setSubtitleFlag(int flag) {
    mDkSession->setSubtitleFlag(flag);
}

void DTVKitClientJni::notify(const parcel_t &parcel) {
    AutoMutex _l(mLock);
    ALOGD("notify msgType = %d  this:%p", parcel.msgType, this);
    if (parcel.msgType == DTVKIT_DRAW) {
        datablock_t datablock;
        datablock.width      = parcel.bodyInt[0];
        datablock.height     = parcel.bodyInt[1];
        datablock.dst_x      = parcel.bodyInt[2];
        datablock.dst_y      = parcel.bodyInt[3];
        datablock.dst_width  = parcel.bodyInt[4];
        datablock.dst_height = parcel.bodyInt[5];

        if (datablock.width != 0 || datablock.height != 0) {
            sp<IMemory> memory = mapMemory(parcel.mem);
            if (memory == nullptr) {
                ALOGE("[%s] memory map is null", __FUNCTION__);
                return;
            }
            uint8_t *data = static_cast<uint8_t*>(static_cast<void*>(memory->getPointer()));
            memory->read();
            memory->commit();
            //int size = memory->getSize();

            postSubtitleData(datablock.width, datablock.height, datablock.dst_x, datablock.dst_y,
            datablock.dst_width, datablock.dst_height, data);
        } else {
            postSubtitleData(datablock.width, datablock.height, datablock.dst_x, datablock.dst_y,
            datablock.dst_width, datablock.dst_height, NULL);
        }
    }

    if (parcel.msgType == REQUEST) {
        dvbparam_t dvbparam;
        dvbparam.resource = parcel.bodyString[0];
        dvbparam.json     = parcel.bodyString[1];
        dvbparam.id       = parcel.bodyInt[0];
        postDvbParam(dvbparam.resource, dvbparam.json, dvbparam.id);
    }

    if (parcel.msgType == SUBSERVER_DRAW) {
        switch (parcel.funname) {
            case SUBTITLE_START:
            {
                ALOGD("funname =%d, isdvbsubt = %d, pid = %d,  subt_type = %d, cpage = %d, apage = %d", parcel.funname,
                parcel.is_dvb_subt, parcel.pid, parcel.subt_type, parcel.subt.cpage, parcel.subt.apage);
                if (parcel.is_dvb_subt || parcel.pid == 0) { //pid =0 defaul cc
                    setSubtitleOn(parcel.pid, parcel.subt_type, parcel.subt.cpage, parcel.subt.apage, parcel.demux_num);
                } else {
                    ALOGD("parcel.ttxt.magazine = %d, parcel.ttxt.page = %d", parcel.ttxt.magazine, parcel.ttxt.page);
                    setSubtitleOn(parcel.pid, parcel.subt_type, parcel.ttxt.magazine, parcel.ttxt.page,
                    parcel.demux_num);
                }
            }
            break;
            case SUBTITLE_STOP:
                setSubtitleOff();
            break;
            case SUBTITLE_PAUSE:
                setSubtitlePause();
            break;
            case SUBTITLE_RESUME:
                setSubtitleResume();
            break;
            case TELETEXT_EVENT:
                ALOGD("event type = %d", parcel.event_type);
                notitySubtitleTeletextEvent(parcel.event_type);
            break;
            case SUBTITLE_TUNE:
                subtitleTune(parcel.bodyInt[0], parcel.bodyInt[1],
                             parcel.bodyInt[2], parcel.bodyInt[3]);
            break;
            default:
                ALOGD("get funname = %d", parcel.funname);
            break;
        }
    }
}

static void getSubtitleListenerImpl() {
    if (mSubContext == nullptr) {
        mSubContext = new SubtitleServerClient(false, new SubtitleDataListenerImpl(), OpenType::TYPE_APPSDK);
    }
}

static void connectdtvkit(JNIEnv *env, jclass clazz __unused, jobject obj)
{
    ALOGI("ref dtvkit");
    mpDtvkitJni  =  DTVKitClientJni::GetInstance();
    DtvkitObject = env->NewGlobalRef(obj);
}

static void disconnectdtvkit(JNIEnv *env, jclass clazz __unused)
{
    ALOGI("disconnect dtvkit");
    env->DeleteGlobalRef(DtvkitObject);
}

static jstring request(JNIEnv *env, jclass clazz __unused, jstring jresource, jstring jjson) {
    const char *resource = env->GetStringUTFChars(jresource, nullptr);
    const char *json = env->GetStringUTFChars(jjson, nullptr);
    if (mpDtvkitJni == nullptr) {
        ALOGE("dtvkitJni is null");
        mpDtvkitJni  =  DTVKitClientJni::GetInstance();
    }
    std::string result   = mpDtvkitJni->request(resource, json);
    env->ReleaseStringUTFChars(jresource, resource);
    env->ReleaseStringUTFChars(jjson, json);
    return env->NewStringUTF(result.c_str());
}

static void setSubtitleOff()
{
    if (mSubContext != nullptr)
    {
        ALOGD("SubtitleServiceCtl:setSubtitleOff to subtitle client.");
        setSubtitleStatus(false);
        mSubContext->close();
    }
    clearSubtitleDataEx();
}

static void setSubtitleOn(int pid, int type, int magazine, int page, int demuxId)
{
    if (mSubContext != nullptr) {
        setSubtitleOff();
        if (type == START_SUB_TYOE_CLOSED_CAPTION) {
            clearCCSubtitleData();
        }
    }
    ALOGD("SubtitleServiceCtl:setSubtitleOn with.pid=%d, type=%d,magazine=%d, page=%d, demuxId = %d.",
        pid, type, magazine, page, demuxId);

    setSubtitleStatus(true);
    if (type == SUBTITLE_SUB_TYPE_TTX || type == SUBTITLE_SUB_TYPE_TTXSUB) {
        mSubContext->setSubType(SUBTITLE_SUB_TYPE_TTXSUB + DTVKIT_SUBTITLE_ADD_OFFSET);
    } else {
        mSubContext->setSubType(type + DTVKIT_SUBTITLE_ADD_OFFSET);
    }
    if (type == START_SUB_TYOE_CLOSED_CAPTION) {
      mSubContext->selectCcChannel(pid);
    } else {
      mSubContext->setSubPid(pid);
    }
    int iotType = SUBTITLE_DEMUX_SOURCE;
    //if (type == SUBTITLE_SUB_TYPE_SCTE) {
    //    iotType = SUBTITLE_SCTE27_SOURCE;
    //}
    if (demuxId != 0) {
        iotType = (demuxId << 16 | iotType);
    }
    if (mSubContext->open("", iotType))
    {
        if ((type == SUBTITLE_SUB_TYPE_TTX)) {
            mSubContext->ttControl(TT_EVENT_GO_TO_PAGE, magazine, page, 0, 0);
        } else if (type == SUBTITLE_SUB_TYPE_TTXSUB) {
            mSubContext->ttControl(TT_EVENT_GO_TO_SUBTITLE, magazine, page, 0, 0);
        }
    }

}

static void notitySubtitleTeletextEvent(int eventType)
{
    ALOGD("notitySubtitleTeletextEvent event:%d", eventType);

    if (mSubContext != nullptr)
    {
        mSubContext->ttControl(eventType, -1, -1, -1, -1); //now subtitleserver according to event to control
    }
}

static void setSubtitlePause()
{
   bool attached = false;
    if (mSubContext != nullptr)
    {
        ALOGD("SubtitleServiceCtl:setSubtitlePause to subtitle client.");
        JNIEnv *env = getJniEnv(&attached);
        if (env != NULL) {
            env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, 0, 0, 0, 0, 0, 9999, 0, NULL);
            env->CallVoidMethod(DtvkitObject, notifySubtitleCallbackEx, 0, 0, 0, 0, 0, 0, 9999, NULL);
            //when play the pvr file and turn on the subtitle, then seek and switch subtitle track,
            //the dvbcore don't deliver the resume.And it cause the subitle hide.
            //env->CallVoidMethod(DtvkitObject, notifySubtitleCbCtlEx, 1);
        }

        if (attached) {
            DetachJniEnv();
        }
    }
}

static void setSubtitleResume()
{
   bool attached = false;
    if (mSubContext != nullptr)
    {
        ALOGD("SubtitleServiceCtl:setSubtitleResume to subtitle client.");
        JNIEnv *env = getJniEnv(&attached);
        if (env != NULL) {
            env->CallVoidMethod(DtvkitObject, notifySubtitleCbCtlEx, 0);
        }

        if (attached) {
            DetachJniEnv();
        }
    }
}

static void subtitleTune(int type, int param1, int param2, int param3)
{
   ALOGD("SubtitleServiceCtl: subtitleTune(type:%d, params:%d,%d,%d)", type, param1, param2, param3);
   if (mSubContext != nullptr)
   {
      mSubContext->setPipId(type + 1, param1);
   }
}

static void attachSubtitleCtl(JNIEnv *env, jclass clazz __unused, jint flag)
{
    ALOGV("SubtitleServiceCtl:attachSubtitleCtl.");
    getSubtitleListenerImpl();
    if (mpDtvkitJni != NULL)
        mpDtvkitJni->setSubtitleFlag(flag);
}

static void detachSubtitleCtl(JNIEnv *env, jclass clazz __unused)
{
   ALOGV("SubtitleServiceCtl:detachSubtitleCtl.");
   if (mpDtvkitJni != NULL)
        mpDtvkitJni->setSubtitleFlag(0);
}

static bool getIsdbtSupport(JNIEnv *env, jclass clazz __unused)
{
#ifdef SUPPORT_ISDBT
    return true;
#else
    return false;
#endif
}

static void nativeUnCrypt(JNIEnv *env, jclass clazz, jstring src, jstring dest) {
    const char *FONT_VENDOR_LIB = "/vendor/lib/libvendorfont.so";
    const char *FONT_PRODUCT_LIB = "/product/lib/libvendorfont.so";

    // TODO: maybe we need some smart method to get the lib.
    void *handle = dlopen(FONT_PRODUCT_LIB, RTLD_NOW);
    if (handle == nullptr) {
        handle = dlopen(FONT_VENDOR_LIB, RTLD_NOW);
    }

    if (handle == nullptr) {
        ALOGE(" nativeUnCrypt error! cannot open uncrypto lib");
    }

    typedef void (*fnFontRelease)(const char*, const char*);
    fnFontRelease fn = (fnFontRelease)dlsym(handle, "vendor_font_release");
    if (fn == nullptr) {
        ALOGE(" nativeUnCrypt error! cannot locate symbol vendor_font_release in uncrypto lib");
        dlclose(handle);
        return;
    }

    const char *srcstr = (const char *)env->GetStringUTFChars(src, NULL);
    const char *deststr = (const char *)env->GetStringUTFChars(dest, NULL);

    fn(srcstr, deststr);
    dlclose(handle);

    (env)->ReleaseStringUTFChars(src, (const char *)srcstr);
    (env)->ReleaseStringUTFChars(dest, (const char *)deststr);
}

/*
static int updateNative(sp<ANativeWindow> nativeWin) {
    char* vaddr;
    int ret = 0;
    ANativeWindowBuffer* buf;

    if (nativeWin.get() == NULL) {
        return 0;
    }

    int err = nativeWin->dequeueBuffer_DEPRECATED(nativeWin.get(), &buf);

    if (err != 0) {
        ALOGE("dequeueBuffer failed: %s (%d)", strerror(-err), -err);
        return -1;
    }

    return nativeWin->queueBuffer_DEPRECATED(nativeWin.get(), buf);
}

static void SetSurface(JNIEnv *env, jclass thiz, jobject jsurface) {
    sp<IGraphicBufferProducer> new_st = NULL;

    if (jsurface) {
        sp<Surface> surface(android_view_Surface_getSurface(env, jsurface));

        if (surface != NULL) {
            new_st = surface->getIGraphicBufferProducer();

            if (new_st == NULL) {
                jniThrowException(env, "java/lang/IllegalArgumentException",
                                  "The surface does not have a binding SurfaceTexture!");
                return;
            }
        } else {
            jniThrowException(env, "java/lang/IllegalArgumentException",
                              "The surface has been released");
            return;
        }
    }

    sp<ANativeWindow> tmpWindow = NULL;

    if (new_st != NULL) {
        tmpWindow = new Surface(new_st);
        status_t err = native_window_api_connect(tmpWindow.get(),
                       NATIVE_WINDOW_API_MEDIA);
        ALOGI("set native window overlay");
        native_window_set_usage(tmpWindow.get(),
                                am_gralloc_get_video_overlay_producer_usage());
        //native_window_set_usage(tmpWindow.get(), GRALLOC_USAGE_HW_TEXTURE |
        //   GRALLOC_USAGE_EXTERNAL_DISP  | GRALLOC1_PRODUCER_USAGE_VIDEO_DECODER );
        native_window_set_buffers_format(tmpWindow.get(), WINDOW_FORMAT_RGBA_8888);

        updateNative(tmpWindow);
    }
}
*/
static void openUserData() {
    if (mSubContext != nullptr) {
        mSubContext->userDataOpen();
    }
}

static void setRegionId(JNIEnv *env, jclass clazz __unused, jint regionId) {
    ALOGD("set region Id:%d", regionId);
    if (mSubContext != nullptr) {
        mSubContext->ttControl(TT_EVENT_SET_REGION_ID, -1, -1, regionId, -1);
    }
}

static void closeUserData() {
    if (mSubContext != nullptr) {
        mSubContext->userDataClose();
    }
}

static void resetForSeek() {
    if (mSubContext != nullptr) {
        mSubContext->resetForSeek();
    }
}

static JNINativeMethod gMethods[] = {
{
    "nativeconnectdtvkit", "(Lorg/droidlogic/dtvkit/DtvkitGlueClient;)V",
    (void *) connectdtvkit
},
{
    "nativedisconnectdtvkit", "()V",
    (void *) disconnectdtvkit
},
/*
{
    "nativeSetSurface", "(Landroid/view/Surface;)V",
    (void *) SetSurface
},
*/
{
    "nativerequest", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
    (void*) request
},
{
    "native_attachSubtitleCtl", "(I)V",
    (void*) attachSubtitleCtl
},
{
   "native_detachSubtitleCtl", "()V",
   (void*) detachSubtitleCtl
},
{
    "native_UnCrypt", "(Ljava/lang/String;Ljava/lang/String;)V",
    (void *)nativeUnCrypt
},
{
    "nativeIsdbtSupport", "()Z",
    (void*) getIsdbtSupport
},
{
   "native_openUserData", "()V",
   (void*) openUserData
},
{
  "native_closeUserData", "()V",
  (void*) closeUserData
},
{
  "native_nativeSubtitleSeekReset", "()V",
  (void*) resetForSeek
},
{
  "native_setRegionId", "(I)V",
  (void*) setRegionId
},

};


#define FIND_CLASS(var, className) \
        var = env->FindClass(className); \
        LOG_FATAL_IF(! var, "Unable to find class " className);

#define GET_METHOD_ID(var, clazz, methodName, methodDescriptor) \
        var = env->GetMethodID(clazz, methodName, methodDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find method " methodName);

int register_org_droidlogic_dtvkit_DtvkitGlueClient(JNIEnv *env)
{
    static const char *const kClassPathName = "org/droidlogic/dtvkit/DtvkitGlueClient";
    jclass clazz;
    int rc;
    FIND_CLASS(clazz, kClassPathName);

    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'\n", kClassPathName);
        return -1;
    }

    rc = (env->RegisterNatives(clazz, gMethods, NELEM(gMethods)));
    if (rc < 0) {
        env->DeleteLocalRef(clazz);
        ALOGE("RegisterNatives failed for '%s' %d\n", kClassPathName, rc);
        return -1;
    }

    GET_METHOD_ID(notifyDvbCallback, clazz, "notifyDvbCallback", "(Ljava/lang/String;Ljava/lang/String;I)V");
    GET_METHOD_ID(notifySubtitleCallback, clazz, "notifySubtitleCallback", "(IIIIII[I)V");
    GET_METHOD_ID(notifySubtitleCallbackEx, clazz, "notifySubtitleCallbackEx", "(IIIIIII[I)V");
    GET_METHOD_ID(notifySubtitleCbCtlEx, clazz, "notifySubtitleCbCtlEx", "(I)V");
    GET_METHOD_ID(notifyCCSubtitleCallbackEx, clazz, "notifyCCSubtitleCallbackEx", "(ZLjava/lang/String;)V");
    GET_METHOD_ID(notifyMixVideoEventCallback, clazz, "notifyMixVideoEventCallback", "(I)V");
    return rc;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved __unused)
{
    JNIEnv *env = NULL;
    jint result = -1;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK)
    {
        ALOGI("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);
    gJavaVM = vm;
    if (register_org_droidlogic_dtvkit_DtvkitGlueClient(env) < 0)
    {
        ALOGE("Can't register DtvkitGlueClient");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}


