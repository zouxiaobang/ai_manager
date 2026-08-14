//
// Created by kyle on 2026/4/12.
//

#ifndef AI_HOUSEKEEPER_KYLEBOARD_H
#define AI_HOUSEKEEPER_KYLEBOARD_H
#include "../../event.h"

class KyleBoard {
public:
    virtual void handleEvent(GlobalEvent event, const char* text = "") = 0;
};



#endif //AI_HOUSEKEEPER_KYLEBOARD_H
