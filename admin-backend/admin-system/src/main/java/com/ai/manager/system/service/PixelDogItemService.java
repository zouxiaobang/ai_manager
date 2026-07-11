package com.ai.manager.system.service;

import com.ai.manager.system.domain.entity.PixelDogItem;
import com.ai.manager.system.domain.vo.PixelDogItemVO;
import java.util.List;

public interface PixelDogItemService {
    List<PixelDogItemVO> listItems();
    PixelDogItemVO createItem(PixelDogItem item);
    PixelDogItemVO updateItem(PixelDogItem item);
    void deleteItem(Long id);
}
