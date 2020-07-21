#参数加密
cmake_minimum_required(VERSION 3.4.1)

find_library( log-lib
              log )

add_library( dkcloudid_crypt
             SHARED
             src/main/cpp/aes.c
             src/main/cpp/dk_crypt.c
             src/main/cpp/msgCrypt.c
             src/main/cpp/platform_util.c)

# 将预构建库与本地库相连
target_link_libraries( dkcloudid_crypt
                       ${log-lib} )