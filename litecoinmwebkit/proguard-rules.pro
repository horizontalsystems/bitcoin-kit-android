# JNI entry points
-keep class io.horizontalsystems.litecoinmwebkit.MwebNative { *; }
-keep class io.horizontalsystems.litecoinmwebkit.MwebNativeException { *; }

# protobuf-lite generated messages are accessed reflectively
-keep class io.horizontalsystems.litecoinmwebkit.proto.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# gRPC
-dontwarn io.grpc.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
