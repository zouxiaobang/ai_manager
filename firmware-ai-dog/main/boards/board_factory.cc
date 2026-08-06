#include "boards/board_factory.h"

#include "boards/kyle-s3-lcd/kyle_s3_lcd_board.h"
#include "boards/supermini-c3/supermini_c3_board.h"

namespace xiaozhi {

IBoard* CreateBoard() {
#if defined(CONFIG_BOARD_SUPERMINI_C3)
    return new SuperminiC3Board();
#elif defined(CONFIG_BOARD_KYLE_S3_LCD)
    return new KyleS3LcdBoard();
#else
#error "未选择任何板型：请在 menuconfig 的 Board selection 中选择 supermini-c3 或 kyle-s3-lcd"
#endif
}

}  // namespace xiaozhi
