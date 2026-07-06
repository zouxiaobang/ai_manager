package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface EcSalesOrderMapper extends BaseMapper<EcSalesOrder> {

    /**
     * 按状态统计指定时间范围内各状态订单数、营收和利润
     */
    @Select("SELECT status, COUNT(*) AS count, " +
            "COALESCE(SUM(received_amount), 0) AS revenue, " +
            "COALESCE(SUM(profit_amount), 0) AS profit " +
            "FROM ec_sales_order " +
            "WHERE order_time >= #{start} AND order_time < #{end} AND deleted = 0 " +
            "AND (#{shopId} IS NULL OR shop_id = #{shopId}) " +
            "GROUP BY status")
    List<Map<String, Object>> countOrdersByStatusGroup(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("shopId") Long shopId);
}
