#include "nw_core.h"

#include <SDL2/SDL.h>
#include <cstdlib>
#include <new>

namespace {

// Empacota RGBA para o formato ABGR8888 esperado no texture stream.
uint32_t NW_PackColor(NW_Color color) {
    return (static_cast<uint32_t>(color.a) << 24) |
           (static_cast<uint32_t>(color.b) << 16) |
           (static_cast<uint32_t>(color.g) << 8)  |
           (static_cast<uint32_t>(color.r));
}

} // namespace

bool NW_Init(NW_Window& window, int width, int height, const char* title) {
    window.width = width;
    window.height = height;
    window.running = false;
    window.framebuffer = nullptr;
    window.nativeWindow = nullptr;
    window.nativeRenderer = nullptr;
    window.nativeTexture = nullptr;

    if (width <= 0 || height <= 0 || title == nullptr) {
        return false;
    }

    if (SDL_Init(SDL_INIT_VIDEO) != 0) {
        return false;
    }

    SDL_Window* sdlWindow = SDL_CreateWindow(
        title,
        SDL_WINDOWPOS_CENTERED,
        SDL_WINDOWPOS_CENTERED,
        width,
        height,
        SDL_WINDOW_SHOWN
    );

    if (!sdlWindow) {
        SDL_Quit();
        return false;
    }

    SDL_Renderer* sdlRenderer = SDL_CreateRenderer(
        sdlWindow,
        -1,
        SDL_RENDERER_SOFTWARE
    );

    if (!sdlRenderer) {
        SDL_DestroyWindow(sdlWindow);
        SDL_Quit();
        return false;
    }

    SDL_Texture* sdlTexture = SDL_CreateTexture(
        sdlRenderer,
        SDL_PIXELFORMAT_ABGR8888,
        SDL_TEXTUREACCESS_STREAMING,
        width,
        height
    );

    if (!sdlTexture) {
        SDL_DestroyRenderer(sdlRenderer);
        SDL_DestroyWindow(sdlWindow);
        SDL_Quit();
        return false;
    }

    const size_t pixelCount = static_cast<size_t>(width) * static_cast<size_t>(height);
    window.framebuffer = new (std::nothrow) uint32_t[pixelCount];

    if (!window.framebuffer) {
        SDL_DestroyTexture(sdlTexture);
        SDL_DestroyRenderer(sdlRenderer);
        SDL_DestroyWindow(sdlWindow);
        SDL_Quit();
        return false;
    }

    window.nativeWindow = sdlWindow;
    window.nativeRenderer = sdlRenderer;
    window.nativeTexture = sdlTexture;
    window.running = true;

    NW_Clear(window, NW_Color{0, 0, 0, 255});
    return true;
}

void NW_Shutdown(NW_Window& window) {
    if (window.framebuffer) {
        delete[] window.framebuffer;
        window.framebuffer = nullptr;
    }

    if (window.nativeTexture) {
        SDL_DestroyTexture(static_cast<SDL_Texture*>(window.nativeTexture));
        window.nativeTexture = nullptr;
    }

    if (window.nativeRenderer) {
        SDL_DestroyRenderer(static_cast<SDL_Renderer*>(window.nativeRenderer));
        window.nativeRenderer = nullptr;
    }

    if (window.nativeWindow) {
        SDL_DestroyWindow(static_cast<SDL_Window*>(window.nativeWindow));
        window.nativeWindow = nullptr;
    }

    SDL_Quit();

    window.width = 0;
    window.height = 0;
    window.running = false;
}

bool NW_ShouldClose(NW_Window& window) {
    return !window.running;
}

void NW_PollEvents(NW_Window& window) {
    SDL_Event event;
    while (SDL_PollEvent(&event)) {
        if (event.type == SDL_QUIT) {
            window.running = false;
        }

        if (event.type == SDL_KEYDOWN && event.key.keysym.sym == SDLK_ESCAPE) {
            window.running = false;
        }
    }
}

void NW_Clear(NW_Window& window, NW_Color color) {
    if (!window.framebuffer) {
        return;
    }

    const uint32_t packed = NW_PackColor(color);
    const int totalPixels = window.width * window.height;

    for (int i = 0; i < totalPixels; ++i) {
        window.framebuffer[i] = packed;
    }
}

void NW_DrawPixel(NW_Window& window, int x, int y, NW_Color color) {
    if (!window.framebuffer) {
        return;
    }

    if (x < 0 || x >= window.width || y < 0 || y >= window.height) {
        return;
    }

    window.framebuffer[y * window.width + x] = NW_PackColor(color);
}

void NW_DrawLine(NW_Window& window, NW_Point2 a, NW_Point2 b, NW_Color color) {
    int x0 = a.x;
    int y0 = a.y;
    const int x1 = b.x;
    const int y1 = b.y;

    const int dx = std::abs(x1 - x0);
    const int sx = (x0 < x1) ? 1 : -1;
    const int dy = -std::abs(y1 - y0);
    const int sy = (y0 < y1) ? 1 : -1;

    int err = dx + dy;

    while (true) {
        NW_DrawPixel(window, x0, y0, color);

        if (x0 == x1 && y0 == y1) {
            break;
        }

        const int e2 = 2 * err;

        if (e2 >= dy) {
            err += dy;
            x0 += sx;
        }

        if (e2 <= dx) {
            err += dx;
            y0 += sy;
        }
    }
}

void NW_DrawRect(NW_Window& window, NW_Point2 topLeft, int width, int height, NW_Color color) {
    if (width <= 0 || height <= 0) {
        return;
    }

    const NW_Point2 topRight{topLeft.x + width - 1, topLeft.y};
    const NW_Point2 bottomLeft{topLeft.x, topLeft.y + height - 1};
    const NW_Point2 bottomRight{topLeft.x + width - 1, topLeft.y + height - 1};

    NW_DrawLine(window, topLeft, topRight, color);
    NW_DrawLine(window, topRight, bottomRight, color);
    NW_DrawLine(window, bottomRight, bottomLeft, color);
    NW_DrawLine(window, bottomLeft, topLeft, color);
}

void NW_DrawTriangle(NW_Window& window, NW_Point2 a, NW_Point2 b, NW_Point2 c, NW_Color color) {
    NW_DrawLine(window, a, b, color);
    NW_DrawLine(window, b, c, color);
    NW_DrawLine(window, c, a, color);
}

void NW_Present(NW_Window& window) {
    if (!window.nativeRenderer || !window.nativeTexture || !window.framebuffer) {
        return;
    }

    SDL_Texture* texture = static_cast<SDL_Texture*>(window.nativeTexture);
    SDL_Renderer* renderer = static_cast<SDL_Renderer*>(window.nativeRenderer);

    SDL_UpdateTexture(
        texture,
        nullptr,
        window.framebuffer,
        window.width * static_cast<int>(sizeof(uint32_t))
    );

    SDL_RenderClear(renderer);
    SDL_RenderCopy(renderer, texture, nullptr, nullptr);
    SDL_RenderPresent(renderer);
}
