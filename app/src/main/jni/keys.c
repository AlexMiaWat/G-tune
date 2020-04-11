//
// Created by Voronov_A on 13.03.2019.
//

#include <jni.h>

JNIEXPORT jstring JNICALL
Java_russianapp_tools_guitar_1tunings_DefaultErrorActivity_getSMTPAUTHUSER(JNIEnv *env, jobject instance) {

    return (*env)->NewStringUTF(env, "eWEuYXZvcm9ub3Y=");
}

JNIEXPORT jstring JNICALL
Java_russianapp_tools_guitar_1tunings_DefaultErrorActivity_getSMTPAUTHPWD(JNIEnv *env, jobject instance) {

    return (*env)->NewStringUTF(env, "WWRmaDQ5OWRoMjAyMA==");
}

JNIEXPORT jstring JNICALL
Java_russianapp_tools_guitar_1tunings_DefaultErrorActivity_getEMAILFROM(JNIEnv *env, jobject instance) {

    return (*env)->NewStringUTF(env, "eWEuYXZvcm9ub3ZAeWFuZGV4LnJ1");
}

JNIEXPORT jstring JNICALL
Java_russianapp_tools_guitar_1tunings_DefaultErrorActivity_getEMAILTO(JNIEnv *env, jobject instance) {

    return (*env)->NewStringUTF(env, "cmFib2NoYXlhLnFkQGdtYWlsLmNvbQ==");
}