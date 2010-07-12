#include "nbia_kernels_Scale.h"

int cudaScale(unsigned char* in, int w, int h, int scale, unsigned char *out);

jint Java_nbia_kernels_Scale_scaleRGBA__Ljava_nio_ByteBuffer_2IIILjava_nio_ByteBuffer_2
  (JNIEnv *env, jclass clazz, jobject bufferIn, jint w, jint h, jint scale, jobject bufferOut) 
{ 
	unsigned char *in = (*env)->GetDirectBufferAddress(env, bufferIn);
	unsigned char *out = (*env)->GetDirectBufferAddress(env, bufferOut);

        printf("BUFFER-BUFFER Scaling %p %p\n", in, out);

//        return cudaScale(in, w, h, scale, out);
        return 0; 
}

jint Java_nbia_kernels_Scale_scaleRGBA__Ljava_nio_ByteBuffer_2III_3B
  (JNIEnv *env, jclass clazz, jobject bufferIn, jint w, jint h, jint scale, jbyteArray arrayOut)
{
        return 0; 
}

jint Java_nbia_kernels_Scale_scaleRGBA___3BIIILjava_nio_ByteBuffer_2
  (JNIEnv *env, jclass clazz, jbyteArray bufferIn, jint w, jint h, jint scale, jobject bufferOut) 
{
        return 0; 
}

jint Java_nbia_kernels_Scale_scaleRGBA___3BIII_3B
  (JNIEnv *env, jclass clazz, jbyteArray arrayIn, jint w, jint h, jint scale, jbyteArray arrayOut)
{
       unsigned char *in = (*env)->GetPrimitiveArrayCritical(env, arrayIn, 0);

       if (in == NULL) {
          printf("EEP Failed to retrieve input\n");
          return 1;
       }

       unsigned char *out = (*env)->GetPrimitiveArrayCritical(env, arrayOut, 0);

       if (out == NULL) {
          printf("EEP Failed to retrieve output\n");
         (*env)->ReleasePrimitiveArrayCritical(env, arrayIn, in, JNI_ABORT);
          return 1;
       }

//       printf("ARRAY-ARRAY Scaling %p %p\n", in, out);

       int result = cudaScale(in, w, h, scale, out);

//       printf("Result %d\n", result);

//       int result = 0;

       (*env)->ReleasePrimitiveArrayCritical(env, arrayIn, in, JNI_ABORT);
       (*env)->ReleasePrimitiveArrayCritical(env, arrayOut, out, JNI_COMMIT);
  
       return result;
}

