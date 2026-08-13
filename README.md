# FitCoach Vision

Assistente de execução de exercícios com visão computacional no próprio aparelho.

**Não substitui médico, fisioterapeuta ou profissional de educação física.** É um assistente de execução e acompanhamento — não faz diagnóstico e não avalia lesões.

**Estado atual: Fase 1 de 8.** Câmera, detecção de pose e overlay de esqueleto funcionando. Contagem de repetições, análise de agachamento e voz entram nas Fases 2 e 3.

---

## Como gerar o APK

Não é preciso instalar nada. A cada envio de arquivos para a branch `main`, o GitHub Actions compila e publica o APK sozinho.

**Baixar no celular:**

```
https://github.com/projetos3intelect-design/fitcoach-vision/releases/latest
```

Baixe `fitcoach-vision.apk` em **Assets**, autorize a instalação de fonte desconhecida e instale.

Se a página de Releases estiver vazia, o APK também fica na aba **Actions** → última execução → **Artifacts** → `fitcoach-vision-apk`.

---

## Arquitetura em uma tela

```
CameraX ImageAnalysis (KEEP_ONLY_LATEST, 640×480, RGBA_8888)
   ▼
PoseImageAnalyzer      rotaciona o frame, garante timestamp crescente
   ▼
PoseLandmarkerSource   MediaPipe LIVE_STREAM, GPU com fallback para CPU
   ▼                   ── ÚNICA classe que enxerga pixels ──
PoseFrame              33 landmarks normalizados + world landmarks
   ▼
SessionController      estado da tela, métricas do pipeline
   ▼
PoseOverlay + DiagnosticHud
```

A partir de `PoseFrame` o código é Kotlin puro, sem dependência de Android. É o que vai permitir testar os analisadores de exercício em JUnit comum, sem emulador, alimentados por sequências de landmarks gravadas.

### Estrutura

```
app/src/main/java/br/com/fitcoachvision/
├── MainActivity.kt          entrada
├── AppPreferences.kt        SharedPreferences (DataStore entra na Fase 3)
├── pose/PoseModels.kt       Landmark, PoseFrame, índices, conexões — puro
├── vision/                  câmera e MediaPipe (única camada com Android+ML)
│   ├── VisionConfig.kt      perfis de modelo e medição de desempenho
│   ├── PoseLandmarkerSource.kt
│   └── PoseImageAnalyzer.kt
└── ui/
    ├── AppRoot.kt           navegação por estado
    ├── theme/Theme.kt
    ├── onboarding/          permissão + disclaimer + privacidade
    ├── home/                treino do dia (dados fixos nesta fase)
    └── session/             câmera, overlay, HUD
```

---

## Decisões que parecem estranhas e não são

**Sem Hilt e sem Room nesta fase.** Ambos dependem de KSP, e o acoplamento `Kotlin ↔ KSP ↔ AGP ↔ Hilt` é a causa mais comum de build quebrado. Como cada build custa ~5 minutos aqui, o projeto inicial resolve apenas Compose, CameraX e MediaPipe — três dependências sem geração de código. Hilt entra na Fase 4, Room na Fase 5.

**Sem Navigation Compose.** Com três telas, uma variável de estado faz o mesmo trabalho. Vira `NavHost` na Fase 5.

**`keystore/debug.keystore` versionado.** O runner do GitHub é descartado a cada build; sem uma chave fixa, cada versão teria assinatura diferente e o Android recusaria a atualização com `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. É uma chave de depuração pública — **não serve para publicar na Play Store**, onde uma chave de release privada é obrigatória.

**Sem permissão de internet.** O `AndroidManifest.xml` não declara `android.permission.INTERNET`. O app é tecnicamente incapaz de enviar qualquer coisa para fora do aparelho — garantia verificável, não promessa.

**A imagem da câmera frontal não é espelhada antes da análise.** Espelhar trocaria o significado anatômico de esquerda e direita nos landmarks, o que quebraria a detecção de assimetria nas próximas fases. O espelhamento acontece só no desenho do overlay.

---

## Se o build falhar

Erro de versão é esperado na primeira execução. Vá em **Actions** → execução vermelha → job `build` → passo com ❌, copie o **texto** do erro.

### Versões usadas

Se o erro for de dependência não encontrada ou de incompatibilidade de plugin, o ajuste é em `gradle/libs.versions.toml`.

| Componente | Versão |
|---|---|
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.14.3 |
| Kotlin | 2.3.21 |
| Compose Compiler Plugin | 2.3.21 (sempre igual ao Kotlin) |
| Compose BOM | 2026.06.01 |
| activity-compose | 1.13.0 |
| CameraX | 1.6.1 |
| MediaPipe Tasks Vision | 0.10.35 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 29 |

**Por que AGP 8.13 e não o 9.x.** O AGP 9 passou a embutir o Kotlin e a dispensar o plugin `org.jetbrains.kotlin.android`, mas a versão do Kotlin passa a vir de dentro do próprio AGP — o que torna difícil garantir que ela case com a versão do plugin do compilador Compose, que precisa ser idêntica. A linha 8.13 permite fixar Kotlin e Compose Compiler explicitamente na mesma versão. A migração para AGP 9 fica para a Fase 8, quando já houver um build verde de referência para comparar.

### Erros comuns

| Erro | Causa | Correção |
|---|---|---|
| `Permission denied: ./gradlew` | O upload pelo site não preserva permissão de execução | Já tratado no workflow pelo passo `chmod +x` |
| `Compose Compiler Gradle plugin is required` | Plugin do Compose não aplicado | Confira `alias(libs.plugins.kotlin.compose)` em `app/build.gradle.kts` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` no celular | Assinatura diferente da versão instalada | Desinstale a versão antiga e instale de novo |
| `failed to load model` ao abrir a câmera | Modelo `.task` ausente ou comprimido | Confira o passo "Baixar modelos" e `noCompress += "task"` |
| O workflow não roda | Pasta `.github` não subiu no upload | Crie o arquivo manualmente em `.github/workflows/build.yml` |

---

## O que testar nesta fase

1. O app abre, mostra o disclaimer e pede a câmera.
2. A tela inicial aparece com o treino de exemplo.
3. Ao abrir a câmera, o esqueleto acompanha o corpo.
4. O HUD mostra fps, latência, confiança, modelo e se está em GPU ou CPU.
5. Trocar entre câmera frontal e traseira funciona.
6. Trocar entre `lite` e `full` muda a latência de forma visível.

**Anote o fps e a latência do HUD nas duas configurações** — esses números definem os padrões das próximas fases.
