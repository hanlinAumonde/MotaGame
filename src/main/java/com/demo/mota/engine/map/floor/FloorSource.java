package com.demo.mota.engine.map.floor;

/**
 * 楼层从哪来。引擎只认这个接口，对存储格式无感：内容层提供 JSON 实现（将来还有 Tiled），
 * 测试可以直接用内存里的楼层。
 */
@FunctionalInterface
public interface FloorSource {

    /**
     * @throws RuntimeException 楼层不存在或数据有误——属于打包错误，不做降级
     */
    FloorDefinition load(int floor);
}
