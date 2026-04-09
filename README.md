# NewWindow (V0)

API gráfica mínima em C++ com renderização por software em framebuffer próprio.

## Arquivos
- `nw_math.h/.cpp`: tipos e base matemática.
- `nw_core.h/.cpp`: API pública `NW_` e backend SDL2 (interno).
- `main.cpp`: exemplo de uso.

## Build (Linux)
```bash
g++ -std=c++17 -Wall -Wextra -pedantic main.cpp nw_core.cpp nw_math.cpp -o newwindow_v0 $(sdl2-config --cflags --libs)
```

## Execução
```bash
./newwindow_v0
```

## API atual
- `NW_Init`, `NW_Shutdown`, `NW_ShouldClose`, `NW_PollEvents`
- `NW_Clear`, `NW_DrawPixel`, `NW_DrawLine`
- `NW_DrawRect`, `NW_DrawTriangle`
- `NW_Present`
