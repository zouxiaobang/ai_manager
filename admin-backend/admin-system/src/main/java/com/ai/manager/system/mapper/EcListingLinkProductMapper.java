package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.EcListingLinkProduct;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EcListingLinkProductMapper extends BaseMapper<EcListingLinkProduct> {

    @Delete("DELETE FROM ec_listing_link_product WHERE link_id = #{linkId}")
    int physicalDeleteByLinkId(@Param("linkId") Long linkId);
}
