#ifndef NW_CORE_H
#define NW_CORE_H

#include <cstdint>
#include "nw_math.h"

// Cor RGBA de 8 bits por canal.
struct NW_Color {
    uint8_t r;
    uint8_t g;
    uint8_t b;
    uint8_t a;
};

// Janela e estado interno da API NewWindow.
// SDL2 fica oculto em ponteiros opacos.
struct NW_Window {
    int width;
    int height;
    bool running;

    uint32_t* framebuffer;

    void* nativeWindow;
    void* nativeRenderer;
    void* nativeTexture;
};

bool NW_Init(NW_Window& window, int width, int height, const char* title);
void NW_Shutdown(NW_Window& window);
bool NW_ShouldClose(NW_Window& window);
void NW_PollEvents(NW_Window& window);
void NW_Clear(NW_Window& window, NW_Color color);
void NW_DrawPixel(NW_Window& window, int x, int y, NW_Color color);
void NW_DrawLine(NW_Window& window, NW_Point2 a, NW_Point2 b, NW_Color color);

// Novas primitivas base para crescer a API sem complicar a V0.
void NW_DrawRect(NW_Window& window, NW_Point2 topLeft, int width, int height, NW_Color color);
void NW_DrawTriangle(NW_Window& window, NW_Point2 a, NW_Point2 b, NW_Point2 c, NW_Color color);

void NW_Present(NW_Window& window);

#endif // NW_CORE_H
