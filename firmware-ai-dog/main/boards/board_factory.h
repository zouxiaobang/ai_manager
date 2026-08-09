#ifndef KYLE_BOARDS_BOARD_FACTORY_H
#define KYLE_BOARDS_BOARD_FACTORY_H

#include "hal/board.h"

namespace kyle {

// 构建时按 Kconfig（CONFIG_BOARD_*）二选一，返回具体板子实例。
// 业务层只拿 IBoard*，不感知具体板型。
IBoard* CreateBoard();

}  // namespace kyle

#endif  // KYLE_BOARDS_BOARD_FACTORY_H
