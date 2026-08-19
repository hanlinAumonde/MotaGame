package com.demo.mota.engine.resource.provider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 资源提供者接口：抽象"从哪里读取资源"。
 * <p>
 * ResourceManager 维护一条 provider 链，读取资源时按下标顺序依次尝试，
 * 前面的 provider 找不到时继续尝试后面的，全部找不到才判定资源不存在。
 * 由此可以把资源来源（类路径、项目外目录、远程等）与使用方解耦，
 * 日后新增读取方式只需实现本接口并注册，无需改动任何读取代码。
 * <p>
 * 路径约定：与类路径资源一致的风格，以 / 开头表示资源根目录，
 * 例如 /data/map/floor_1.json、/images/magictower.png。
 */
public interface ResourceProvider {

    /**
     * 打开指定路径的资源流。
     *
     * @param resourcePath 资源路径（/ 开头）
     * @return 资源存在时返回其输入流；不存在时返回 null（不抛异常，便于 ResourceManager 继续尝试下一个 provider）
     * @throws IOException 资源存在但读取失败时抛出
     */
    InputStream openStream(String resourcePath) throws IOException;

    /**
     * 返回资源的 URL，供 FXMLLoader 等需要 URL 定位的组件使用。
     *
     * @param resourcePath 资源路径（/ 开头）
     * @return 资源存在时返回其 URL；不存在时返回 null
     */
    URL getResourceUrl(String resourcePath);
}
