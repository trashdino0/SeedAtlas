#include <jni.h>
#include <stdint.h>

#include "generator.h"
#include "finders.h"
#include "util.h"

JNIEXPORT jint JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_getBiomeAt(
        JNIEnv *env, jclass clazz,
        jlong seed, jint x, jint y, jint z,
        jint scale, jint mc, jint flags)
{
    Generator g;
    setupGenerator(&g, (int) mc, (uint32_t) flags);
    applySeed(&g, DIM_OVERWORLD, (uint64_t) seed);
    return (jint) getBiomeAt(&g, (int) scale, (int) x, (int) y, (int) z);
}

JNIEXPORT jboolean JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_getStructurePos(
        JNIEnv *env, jclass clazz,
        jint type, jint mc, jlong seed48,
        jint regX, jint regZ, jintArray out)
{
    if (out == NULL || (*env)->GetArrayLength(env, out) < 2)
        return JNI_FALSE;

    Pos p;
    if (!getStructurePos((int) type, (int) mc, (uint64_t) seed48, (int) regX, (int) regZ, &p))
        return JNI_FALSE;

    jint values[2] = { (jint) p.x, (jint) p.z };
    (*env)->SetIntArrayRegion(env, out, 0, 2, values);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_getStructureConfig(
        JNIEnv *env, jclass clazz,
        jint type, jint mc, jlongArray out)
{
    if (out == NULL || (*env)->GetArrayLength(env, out) < 4)
        return JNI_FALSE;

    StructureConfig sc;
    if (!getStructureConfig((int) type, (int) mc, &sc))
        return JNI_FALSE;

    jlong values[4] = {
            (jlong) (uint32_t) sc.salt,
            (jlong) sc.regionSize,
            (jlong) sc.chunkRange,
            (jlong) sc.structType
    };
    (*env)->SetLongArrayRegion(env, out, 0, 4, values);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_isViableStructurePos(
        JNIEnv *env, jclass clazz,
        jint type, jint mc, jlong seed,
        jint x, jint z, jint flags)
{
    Generator g;
    setupGenerator(&g, (int) mc, (uint32_t) flags);
    applySeed(&g, DIM_OVERWORLD, (uint64_t) seed);
    return isViableStructurePos((int) type, &g, (int) x, (int) z, 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_strongholds(
        JNIEnv *env, jclass clazz,
        jint mc, jlong seed, jint count, jintArray out)
{
    if (out == NULL || count <= 0 || (*env)->GetArrayLength(env, out) < count * 2)
        return 0;

    Generator g;
    StrongholdIter sh;
    setupGenerator(&g, (int) mc, 0);
    applySeed(&g, DIM_OVERWORLD, (uint64_t) seed);

    Pos p = initFirstStronghold(&sh, (int) mc, (uint64_t) seed);
    jint values[2] = { (jint) p.x, (jint) p.z };
    (*env)->SetIntArrayRegion(env, out, 0, 2, values);

    int found = 1;
    while (found < (int) count)
    {
        int result = nextStronghold(&sh, &g);
        if (result <= 0)
            break;
        jint next[2] = { (jint) sh.pos.x, (jint) sh.pos.z };
        (*env)->SetIntArrayRegion(env, out, found * 2, 2, next);
        found++;
    }
    return (jint) found;
}

JNIEXPORT jint JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_findStructures(
        JNIEnv *env, jclass clazz,
        jint type, jint mc, jlong seed,
        jint regMinX, jint regMinZ, jint regMaxX, jint regMaxZ,
        jintArray out)
{
    if (out == NULL || regMaxX < regMinX || regMaxZ < regMinZ)
        return 0;

    Generator g;
    setupGenerator(&g, (int) mc, 0);
    applySeed(&g, DIM_OVERWORLD, (uint64_t) seed);

    jsize capacity = (*env)->GetArrayLength(env, out) / 2;
    jint *values = (*env)->GetIntArrayElements(env, out, NULL);
    if (values == NULL)
        return 0;

    int found = 0;
    for (int regZ = (int) regMinZ; regZ <= (int) regMaxZ; regZ++)
    {
        for (int regX = (int) regMinX; regX <= (int) regMaxX; regX++)
        {
            if (found >= (int) capacity)
                break;

            Pos p;
            if (!getStructurePos((int) type, (int) mc, (uint64_t) seed, regX, regZ, &p))
                continue;
            if (!isViableStructurePos((int) type, &g, p.x, p.z, 0))
                continue;

            values[found * 2] = (jint) p.x;
            values[found * 2 + 1] = (jint) p.z;
            found++;
        }
    }

    (*env)->ReleaseIntArrayElements(env, out, values, 0);
    return (jint) found;
}

JNIEXPORT jint JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_biomesAt(
        JNIEnv *env, jclass clazz,
        jlong seed, jint mc, jint scale, jintArray coords, jintArray out)
{
    if (coords == NULL || out == NULL)
        return 0;

    jsize coordCount = (*env)->GetArrayLength(env, coords);
    jsize capacity = (*env)->GetArrayLength(env, out);
    jsize points = coordCount / 3;
    if (points > capacity)
        points = capacity;
    if (points <= 0)
        return 0;

    Generator g;
    setupGenerator(&g, (int) mc, 0);
    applySeed(&g, DIM_OVERWORLD, (uint64_t) seed);

    jint *xyz = (*env)->GetIntArrayElements(env, coords, NULL);
    jint *biomes = (*env)->GetIntArrayElements(env, out, NULL);
    if (xyz == NULL || biomes == NULL)
    {
        if (xyz) (*env)->ReleaseIntArrayElements(env, coords, xyz, JNI_ABORT);
        if (biomes) (*env)->ReleaseIntArrayElements(env, out, biomes, JNI_ABORT);
        return 0;
    }

    for (jsize i = 0; i < points; i++)
    {
        biomes[i] = (jint) getBiomeAt(
                &g, (int) scale, xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2]);
    }

    (*env)->ReleaseIntArrayElements(env, coords, xyz, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, out, biomes, 0);
    return (jint) points;
}

JNIEXPORT jboolean JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_spawn(
        JNIEnv *env, jclass clazz,
        jint mc, jlong seed, jboolean estimate, jintArray out)
{
    if (out == NULL || (*env)->GetArrayLength(env, out) < 2)
        return JNI_FALSE;

    Generator g;
    setupGenerator(&g, (int) mc, 0);
    applySeed(&g, DIM_OVERWORLD, (uint64_t) seed);

    Pos p = estimate ? estimateSpawn(&g, NULL) : getSpawn(&g);
    jint values[2] = { (jint) p.x, (jint) p.z };
    (*env)->SetIntArrayRegion(env, out, 0, 2, values);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_biomeName(
        JNIEnv *env, jclass clazz, jint mc, jint id)
{
    const char *name = biome2str((int) mc, (int) id);
    if (name == NULL)
        return NULL;
    return (*env)->NewStringUTF(env, name);
}

JNIEXPORT jint JNICALL
Java_dev_seedatlas_nativebridge_CubiomesNative_newestVersion(
        JNIEnv *env, jclass clazz)
{
    return (jint) MC_NEWEST;
}
