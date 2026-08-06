#ifndef XIAOZHI_BOARDS_BOARD_FACTORY_H
#define XIAOZHI_BOARDS_BOARD_FACTORY_H

#include "hal/board.h"

namespace xiaozhi {

// 构建时按 Kconfig（CONFIG_BOARD_*）二选一，返回具体板子实例。
// 业务层只拿 IBoard*，不感知具体板型。
IBoard* CreateBoard();

}  // namespace xiaozhi

#endif  // XIAOZHI_BOARDS_BOARD_FACTORY_H
