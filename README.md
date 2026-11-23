# PurpleBank Simulator

Simulador de banco digital **fictício e independente** (Kotlin · Jetpack Compose · Material 3 · Room · DataStore · MVVM · Navigation Compose).
Sem permissão de internet: funciona 100% offline. Todos os dados são fictícios e ficam só no aparelho.
A faixa **"SIMULADOR — DADOS FICTÍCIOS"** aparece em todas as telas; não há marca, logotipo ou identidade de nenhuma instituição real.

## Compilar (Android Studio)
1. Instale o Android Studio (Koala ou mais novo) com o SDK 34.
2. `File > Open` e escolha esta pasta. Aguarde o *Gradle Sync* (precisa de internet só nessa etapa, para baixar dependências).
3. Para usar `./gradlew` no terminal, gere o wrapper uma vez: `gradle wrapper --gradle-version 8.7` (ou deixe o Android Studio fazer isso).
4. Gerar o APK de demonstração:
   ```
   ./gradlew assembleDebug
   ```
   Saída: `app/build/outputs/apk/debug/app-debug.apk`
5. Testes unitários (cálculos de fatura, parcelas, parser de valores): `./gradlew test`

## Instalar no celular
- Copie o `app-debug.apk` para o aparelho, abra e permita "instalar apps de fontes desconhecidas" quando pedido; **ou**
- Com depuração USB ligada: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Release assinado (distribuição privada)
1. Crie uma chave: `keytool -genkey -v -keystore purplebank-release.jks -alias purplebank -keyalg RSA -keysize 2048 -validity 10000`
2. Copie `keystore.properties.example` para `keystore.properties` e preencha (o arquivo está no `.gitignore`).
3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
Sem `keystore.properties`, o release sai **não assinado**.

## PIN do Painel do Simulador
PIN inicial: **1234** (altere em Configurações > Alterar PIN). Só o hash SHA-256 é guardado (DataStore).
O painel volta a bloquear sempre que você sai dele.

## Como as contas fecham
- Valores em centavos (`Long`), formatação pt-BR (`R$ 1.234,56`).
- Fatura = soma das compras + ajuste manual. Parcelas caem em faturas consecutivas (a 1ª absorve os centavos de resto).
- Compra até o dia de fechamento entra na fatura do mês; depois, na seguinte.
- Limite utilizado = soma das faturas não pagas; limite disponível = limite total − utilizado (recalculado a cada alteração).
- "Pagar com saldo" é simulado: debita o saldo fictício, cria uma movimentação e marca a fatura como paga.
- Novas movimentações podem (opcionalmente) ser aplicadas ao saldo; editar/excluir movimentação não mexe no saldo.

## Estrutura
```
app/src/main/java/com/purplebank/simulator/
  data/   Entities, Daos, AppDatabase (Room), Repository, Prefs (DataStore)
  util/   Fmt (dinheiro/datas), Billing (regras de fatura)
  ui/     MainViewModel, Components, theme/, screens/ (Início, Conta, Cartão+Faturas, Histórico, Configurações, Painel)
```

## Gerar o APK sem Android Studio (GitHub Actions)
1. Crie um repositório no GitHub e envie esta pasta (o `.gitignore` já está pronto).
2. Aba **Actions > Build APK** roda sozinha a cada push (ou use *Run workflow*).
3. Ao terminar, baixe o artefato **app-debug** (contém `app-debug.apk`).
Se o build falhar, o log mostra o erro de compilação exato.
