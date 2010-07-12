#include <cutil_inline.h>
#include <cstdlib>
#include <cstdio>
#include <string.h>

// 2D float texture
texture<uchar4, 2, cudaReadModeElementType> texRef;

// Simple scaling kernel
__global__ void scaleKernel(uchar4* output, int width, int height, int scale, int newW, int newH)
{
    // Calculate normalized texture coordinates
    unsigned int x = threadIdx.x * scale;
    unsigned int y = blockIdx.y * scale;

    float4 result = { 0.0, 0.0, 0.0, 0.0 };
    uchar4 tmp;

    for (unsigned int i=0;i<scale;i++) {
        for (unsigned int j=0;j<scale;j++) {
  
           tmp = tex2D(texRef, x+j, y+i);

           result.x += tmp.x;
           result.y += tmp.y;
           result.z += tmp.z;
           result.w += tmp.w;
        }
    }

    float sqr = scale*scale;

    tmp.x = result.x / sqr;
    tmp.y = result.y / sqr;
    tmp.z = result.z / sqr;
    tmp.w = result.w / sqr;
  
    output[blockIdx.y*newW + threadIdx.x] = tmp;      
}

// Simple scaling kernel
__global__ void scaleKernel16(uchar4* output, int width, int height)
{
   // TODO
}

extern "C" {

int cudaScale(unsigned char* in, int w, int h, int scale, unsigned char *out) 
{ 
    // Describe the texture as 4 unsigned bytes per element
    cudaChannelFormatDesc channelDesc = cudaCreateChannelDesc(8, 8, 8, 8, cudaChannelFormatKindUnsigned);

    // Copy the input data into the device
    cudaArray* inArray;
    cudaMallocArray(&inArray, &channelDesc, w, h);
    cudaMemcpyToArray(inArray, 0, 0, in, w*h*4, cudaMemcpyHostToDevice);

    // Set texture parameters (clamp the coordinates, return single points, and do not normalize)
    texRef.addressMode[0] = cudaAddressModeClamp;
    texRef.addressMode[1] = cudaAddressModeClamp;
    texRef.filterMode     = cudaFilterModePoint;
    texRef.normalized     = false;

    // Bind the array to the texture
    cudaBindTextureToArray(texRef, inArray, channelDesc);

    int newW = w/scale;
    int newH = h/scale;

    // Allocate result output array in device memory
    uchar4* output;
    cudaMalloc((void**)&output, newW * newH * 4);

    // Invoke kernel
    dim3 dimBlock(newW, 1);
    dim3 dimGrid(1, newH);

    scaleKernel<<<dimGrid, dimBlock>>>(output, w, h, scale, newW, newH);

    cudaMemcpy(out, output, newW*newH*4, cudaMemcpyDeviceToHost); 

    // check if kernel invocation generated an error
    cutilCheckMsg("Kernel execution failed");

    // Free device memory
    cudaFreeArray(inArray);
    cudaFree(output);

    return 0;
}

}
