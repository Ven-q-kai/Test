#include "nw_core.h"

int main() {
    NW_Window window{};

    if (!NW_Init(window, 800, 600, "NewWindow V0")) {
        return 1;
    }

    while (!NW_ShouldClose(window)) {
        NW_PollEvents(window);

        NW_Clear(window, NW_Color{20, 20, 28, 255});

        NW_DrawPixel(window, 400, 300, NW_Color{255, 0, 0, 255});

        NW_DrawLine(window, NW_Point2{100, 100}, NW_Point2{700, 500}, NW_Color{0, 255, 255, 255});
        NW_DrawLine(window, NW_Point2{100, 500}, NW_Point2{700, 100}, NW_Color{255, 255, 0, 255});

        // Continuação da V0: novas primitivas de borda.
        NW_DrawRect(window, NW_Point2{250, 180}, 300, 240, NW_Color{255, 128, 0, 255});
        NW_DrawTriangle(
            window,
            NW_Point2{400, 140},
            NW_Point2{560, 420},
            NW_Point2{240, 420},
            NW_Color{120, 255, 120, 255}
        );

        NW_Present(window);
    }

    NW_Shutdown(window);
    return 0;
}
