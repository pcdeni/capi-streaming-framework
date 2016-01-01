
#include "DTOCSThread.h"

#include <stdio.h>
#include <stdlib.h>

#include <fstream>
#include <string>
using namespace std;

// libcxl
extern "C" {
  #include "libcxl.h"
}

#define APP_NAME              "example"

#define CACHELINE_BYTES       128                   // 0x80
#define MMIO_ADDR             0x3fffff8             // 0x3fffff8 >> 2 = 0xfffffe

#ifdef  SIM
  #define DEVICE              "/dev/cxl/afu0.0d"
#else
  #define DEVICE              "/dev/cxl/afu1.0d"
#endif

struct wed {
  __u8   volatile status;      // 7    downto 0
  __u8   wed00_a;              // 15   downto 8
  __u16  wed00_b;              // 31   downto 16
  __u32  wed00_c;              // 63   downto 32
  __u8   *source;              // 127  downto 64
  __u32  *dest;                // 191  downto 128
  __u64  width;                // 255  downto 192
  __u64  height;               // 319  downto 256
  __u64  alpha;                // 383  downto 320
  __u32  *dest_temp;           // 415  downto 384
  __u32  wed07;                // 447  downto 416
  __u64  wed08;                // 511  downto 448
  __u64  wed09;                // 575  downto 512
  __u64  wed10;                // 639  downto 576
  __u64  wed11;                // 703  downto 640
  __u64  wed12;                // 767  downto 704
  __u64  wed13;                // 831  downto 768
  __u64  wed14;                // 895  downto 832
  __u64  wed15;                // 959  downto 896
  __u64  wed16;                // 1023 downto 960
};

__u8 *source_image;
__u32 *dest_image;
__u32 h, w, a;

JNIEXPORT jintArray JNICALL Java_DTOCSThread_DTOCS_1cpp
  (JNIEnv *env, jobject obj, jintArray data, jint width, jint height, jint alpha){

	jsize len_data = env->GetArrayLength(data);
        jint *body_data = env->GetIntArrayElements(data, 0);

	source_image = (__u8) body_data;
	h = height;
	w = width;
	a = alpha;

	transfer();

        env->ReleaseIntArrayElements(array, body, 0);
        return dest_image;

}

void transfer () {

  // open afu device
  struct cxl_afu_h *afu = cxl_afu_open_dev ((char*) (DEVICE));
  if (!afu) {
    perror ("cxl_afu_open_dev");
    return -1;
  }

  // setup wed
  struct wed *wed0 = NULL;
  if (posix_memalign ((void **) &(wed0), CACHELINE_BYTES, sizeof(struct wed))) {
    perror ("posix_memalign");
    return -1;
  }

  __u8 *source = NULL;
  //if (posix_memalign ((void **) &(source), CACHELINE_BYTES, length*width*3*sizeof(char))) {
  if (posix_memalign ((void **) &(source), CACHELINE_BYTES, h*w*sizeof(char))) {
    perror ("posix_memalign");
    return -1;
  }
  // copy source image data to cacheline aligned memory
  //memcpy(body_data, source, h*w*3*sizeof(char));
  memcpy(body_data, source, h*w*sizeof(char));

  wed0->source = source_image;
  wed0->height = (__u64) h;
  wed0->width = (__u64) w;
  wed0->alpha = (__u64) a;

  __u32 *dest = NULL;
  if (posix_memalign ((void **) &(dest), CACHELINE_BYTES, h*w*sizeof(int))) {
    perror ("posix_memalign");
    return -1;
  }

  __u32 *dest_temp = NULL;
  if (posix_memalign ((void **) &(dest_temp), CACHELINE_BYTES, h*w*sizeof(int))) {
    perror ("posix_memalign");
    return -1;
  }

  // touch destination data to prevent allocation penalty
  memset(dest, 0, h*w*3*sizeof(int));
  memset(dest_temp, 4294967295, h*w*sizeof(int));

  wed0->dest = dest;
  wed0->dest_temp = dest_temp;
	
  // attach afu and pass wed address
  if (cxl_afu_attach (afu, (__u64) wed0) < 0) {
    perror ("cxl_afu_attach");
    return -1;
  }

  printf("AFU has started.\n");

  // map mmio
  if ((cxl_mmio_map (afu, CXL_MMIO_BIG_ENDIAN)) < 0) {
    perror("cxl_mmio_map");
    return -1;
  }

  uint64_t rc;

  // wait for afu
  while (!wed0->status) {
    cxl_mmio_read64(afu, MMIO_ADDR, &rc);
    printf("Response counter: %lu\n", rc);
  }

  printf("%d\n", *(dest));
  dest_image = dest_temp;

  printf("AFU is done.\n");

  cxl_mmio_unmap (afu);
  cxl_afu_free (afu);

  return 0;

}

int main (int argc, char *argv[]){

	return 0;

}

