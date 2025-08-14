<template>
  <div id="app">
    <div class="container">
      <header>
        <div class="logo">
          <i class="fas fa-robot"></i>
          <div>
            <h1>AI小冰智能对话</h1>
            <div class="tagline">提出问题，获取优化建议，沟通更高效</div>
          </div>
        </div>
      </header>

      <div class="main-content">
        <div class="conversation-container">
          <div class="conversation-title">
            <i class="fas fa-comments"></i>
            <span>对话记录</span>
          </div>
          <div class="messages" ref="scrollContainer">
            <div
              v-for="(message, index) in messages"
              :key="index"
              :class="[
                'message',
                message.sender === 'user'
                  ? 'user-message'
                  : 'assistant-message',
              ]"
            >
              <div class="message-header">
                <i
                  :class="
                    message.sender === 'user' ? 'fas fa-user' : 'fas fa-robot'
                  "
                ></i>
                <span>{{ message.sender === "user" ? "您" : "AI小冰" }}</span>
              </div>
              <div class="message-content">{{ message.content }}</div>
            </div>

            <div v-if="messages.length === 0" class="empty-conversation">
              <div class="message assistant-message">
                <div class="message-header">
                  <i class="fas fa-robot"></i>
                  <span>AI小冰</span>
                </div>
                <div class="message-content">
                  您好！我是您的智能对话助手。请提出您的问题，我将帮助您优化表达并提供建议。
                </div>
              </div>
            </div>
          </div>

          <div class="tips">
            <div class="tips-title">
              <i class="fas fa-lightbulb"></i>
              <span>使用技巧</span>
            </div>
            <ul>
              <li>尽量清晰地描述您的问题，包括必要的背景信息</li>
              <li>使用"如何"、"为什么"等词语开始您的问题，可获得更好的建议</li>
              <li>
                点击"生成建议"后，系统会优化您的问题并提供更专业的表达方式
              </li>
              <li>在发送前，您可以确认或修改建议内容</li>
            </ul>
          </div>
        </div>

        <div class="input-section">
          <div class="section" style="margin-top: -20px">
            <h2>设备配置</h2>
            <div class="device-info">
              <span
                >MAC: <strong id="displayMac">{{ savedMac }}</strong></span
              >
              <span
                >客户端:
                <strong id="displayClient">web_test_client</strong></span
              >
            </div>
          </div>
          <div class="section" style="margin-top: -20px">
            <h2>连接信息</h2>
            <span class="connection-status">
              <span
                >OTA:
                <span id="otaStatus" class="status">{{ otaStatus }}</span></span
              >
              &nbsp;&nbsp;&nbsp;
              <span
                >WS:
                <span id="connectionStatus" class="status">{{
                  connectionStatus
                }}</span></span
              >
            </span>
          </div>
          <div class="connection-controls">
            <button
              class="generate-btn"
              id="connectButton"
              @click="isConnectToServer"
            >
              {{ connect }}
            </button>
          </div>
          <div class="input-title">
            <i class="fas fa-edit"></i>
            <span>输入您的问题</span>
          </div>
          <div class="input-container">
            <textarea
              v-model="userInput"
              :disabled="isInput"
              placeholder="请输入您的问题，例如：'如何提高团队协作效率？'"
            ></textarea>
          </div>

          <div class="buttons">
            <button class="luyin-btn" @click="recordBtn" ref="recordButton">
              <i class="fas fa-magic"></i>
              开始录音
              <canvas id="audioVisualizer" class="audio-visualizer"></canvas>
            </button>

            <button
              class="send-btn"
              :disabled="isSendmessagebtn"
              ref="sendButton"
              @click="sendMessage"
            >
              <i class="fas fa-paper-plane"></i>
              发送消息
            </button>
            <button class="cancel-btn" @click="resetInput">
              <i class="fas fa-times"></i>
              重置
            </button>
          </div>

          <div v-if="suggestionGenerated" class="suggestion-section">
            <div class="suggestion-header">
              <i class="fas fa-star"></i>
              <span>优化建议</span>
            </div>
            <div class="suggestion-box">
              {{ suggestion }}
            </div>
            <div class="confirmation">
              <i class="fas fa-check-circle" style="color: #1890ff"></i>
              <span>建议已生成！请确认后发送，或直接修改上方输入框内容</span>
            </div>
          </div>

          <div v-if="userInput && !suggestionGenerated" class="tips">
            <div class="tips-title">
              <i class="fas fa-info-circle"></i>
              <span>提示</span>
            </div>
            <p>
              点击<span class="highlight">"生成建议"</span
              >按钮，我将为您的输入提供优化建议，使表达更专业清晰。
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import marked from "marked";
export default {
  name: "chat",
  data() {
    return {
      userInput: "",
      suggestion: "",
      suggestionGenerated: false,
      messages: [],
      savedMac: "",
      otaUrl: "http://192.168.197.207:8002/xiaozhi/ota/", //OTA服务器地址
      serverUrl: "ws://192.168.197.204:8000/xiaozhi/v1/", //WebSocket服务器地址
      otaStatus: "ota未连接",
      connectionStatus: "ws未连接",
      connect: "连接",
      isInput: true, //输入框
      isSendmessagebtn: true, //发送按钮
      isRecording: false, //录音按钮
      // 全局变量
      websocket: null,
      mediaRecorder: null,
      audioContext: null,
      analyser: null,
      audioChunks: [],
      visualizerCanvas: null,
      visualizerContext: null,
      audioQueue: [],
      isPlaying: false,
      opusDecoder: null, // Opus解码器
      visualizationRequest: null, // 动画帧请求ID

      // 音频流缓冲相关
      audioBuffers: [], // 用于存储接收到的所有音频数据
      totalAudioSize: 0, // 跟踪累积的音频大小

      audioBufferQueue: [], // 存储接收到的音频包
      isAudioBuffering: false, // 是否正在缓冲音频
      isAudioPlaying: false, // 是否正在播放音频
      BUFFER_THRESHOLD: 3, // 缓冲包数量阈值，至少累积3个包再开始播放
      MIN_AUDIO_DURATION: 0.1, // 最小音频长度(秒)，小于这个长度的音频会被合并
      streamingContext: null, // 音频流上下文
      SAMPLE_RATE: 16000, // 采样率
      CHANNELS: 1, // 声道数
      FRAME_SIZE: 960, // 帧大小
      opusEncoder: null,
      encoderPtr: null,
      // PCM录音处理器代码 - 会被注入到AudioWorklet中
      audioProcessorCode: `
            class AudioRecorderProcessor extends AudioWorkletProcessor {
                constructor() {
                    super();
                    this.buffers = [];
                    this.frameSize = 960; // 60ms @ 16kHz = 960 samples
                    this.buffer = new Int16Array(this.frameSize);
                    this.bufferIndex = 0;
                    this.isRecording = false;

                    // 监听来自主线程的消息
                    this.port.onmessage = (event) => {
                        if (event.data.command === 'start') {
                            this.isRecording = true;
                            this.port.postMessage({ type: 'status', status: 'started' });
                        } else if (event.data.command === 'stop') {
                            this.isRecording = false;

                            // 发送剩余的缓冲区
                            if (this.bufferIndex > 0) {
                                const finalBuffer = this.buffer.slice(0, this.bufferIndex);
                                this.port.postMessage({
                                    type: 'buffer',
                                    buffer: finalBuffer
                                });
                                this.bufferIndex = 0;
                            }

                            this.port.postMessage({ type: 'status', status: 'stopped' });
                        }
                    };
                }

                process(inputs, outputs, parameters) {
                    if (!this.isRecording) return true;

                    const input = inputs[0][0]; // 获取第一个输入通道
                    if (!input) return true;

                    // 将浮点采样转换为16位整数并存储
                    for (let i = 0; i < input.length; i++) {
                        if (this.bufferIndex >= this.frameSize) {
                            // 缓冲区已满，发送给主线程并重置
                            this.port.postMessage({
                                type: 'buffer',
                                buffer: this.buffer.slice(0)
                            });
                            this.bufferIndex = 0;
                        }

                        // 转换为16位整数 (-32768到32767)
                        this.buffer[this.bufferIndex++] = Math.max(-32768, Math.min(32767, Math.floor(input[i] * 32767)));
                    }

                    return true;
                }
            }

            registerProcessor('audio-recorder-processor', AudioRecorderProcessor);
        `,
      // 初始化直接从PCM数据录音的系统
      audioProcessor: null,
      audioProcessorType: null,
      audioSource: null,
      // 处理PCM缓冲数据
      pcmDataBuffer: new Int16Array(),
    };
  },
  mounted() {
    (this.visualizerCanvas = document.getElementById("audioVisualizer")),
      (this.visualizerContext = this.visualizerCanvas.getContext("2d")),
      this.initApp();
  },
  methods: {
    // 初始化应用
    initApp() {
      this.initEventListeners();
      // 检查libopus.js是否正确加载
      this.checkOpusLoaded();
      // 初始化Opus编码器
      this.initOpusEncoder();
      // 预加载Opus解码器
      console.log("预加载Opus解码器...", "info");
      this.initOpusDecoder()
        .then(() => {
          console.log("Opus解码器预加载成功", "success");
        })
        .catch((error) => {
          console.log(
            `Opus解码器预加载失败: ${error.message}，将在需要时重试`,
            "warning"
          );
        });
    },
    isConnectToServer() {
      if (this.connect == "连接") {
        this.connectToServer();
      } else {
        this.disconnectFromServer();
      }
    },
    // 初始化事件监听器
    initEventListeners() {
      // 从localStorage加载MAC地址，如果没有则生成新的
      this.savedMac = localStorage.getItem("deviceMac");
      if (!this.savedMac) {
        this.savedMac = this.generateRandomMac();
        localStorage.setItem("deviceMac", this.savedMac);
      }
    },
    // 检查Opus库是否已加载
    checkOpusLoaded() {
      try {
        // 检查Module是否存在（本地库导出的全局变量）
        if (typeof Module === "undefined") {
          throw new Error("Opus库未加载，Module对象不存在");
        }

        // 尝试先使用Module.instance（libopus.js最后一行导出方式）
        if (
          typeof Module.instance !== "undefined" &&
          typeof Module.instance._opus_decoder_get_size === "function"
        ) {
          // 使用Module.instance对象替换全局Module对象
          window.ModuleInstance = Module.instance;
          console.log("Opus库加载成功（使用Module.instance）", "success");

          // 3秒后隐藏状态
          const statusElement = document.getElementById("scriptStatus");
          if (statusElement) statusElement.style.display = "none";
          return;
        }

        // 如果没有Module.instance，检查全局Module函数
        if (typeof Module._opus_decoder_get_size === "function") {
          window.ModuleInstance = Module;
          console.log("Opus库加载成功（使用全局Module）", "success");

          // 3秒后隐藏状态
          const statusElement = document.getElementById("scriptStatus");
          if (statusElement) statusElement.style.display = "none";
          return;
        }

        throw new Error("Opus解码函数未找到，可能Module结构不正确");
      } catch (err) {
        console.log(
          `Opus库加载失败，请检查libopus.js文件是否存在且正确: ${err.message}`,
          "error"
        );
      }
    },
    // 创建音频处理器
    async createAudioProcessor() {
      if (!this.audioContext) {
        this.audioContext = new (window.AudioContext ||
          window.webkitAudioContext)({
          sampleRate: 16000,
          latencyHint: "interactive",
        });
      }

      try {
        // 检查是否支持AudioWorklet
        if (this.audioContext.audioWorklet) {
          // 注册音频处理器
          const blob = new Blob([this.audioProcessorCode], {
            type: "application/javascript",
          });
          const url = URL.createObjectURL(blob);
          await this.audioContext.audioWorklet.addModule(url);
          URL.revokeObjectURL(url);

          // 创建音频处理节点
          const audioProcessor = new AudioWorkletNode(
            this.audioContext,
            "audio-recorder-processor"
          );

          // 设置音频处理消息处理
          audioProcessor.port.onmessage = (event) => {
            if (event.data.type === "buffer") {
              // 收到PCM缓冲区数据
              this.processPCMBuffer(event.data.buffer);
            }
          };

          console.log("使用AudioWorklet处理音频", "success");
          return { node: audioProcessor, type: "worklet" };
        } else {
          // 使用旧版ScriptProcessorNode作为回退方案
          console.log(
            "AudioWorklet不可用，使用ScriptProcessorNode作为回退方案",
            "warning"
          );

          const frameSize = 4096; // ScriptProcessorNode缓冲区大小
          const scriptProcessor = this.audioContext.createScriptProcessor(
            frameSize,
            1,
            1
          );

          // 将audioProcess事件设置为处理音频数据
          scriptProcessor.onaudioprocess = (event) => {
            if (!this.isRecording) return;

            const input = event.inputBuffer.getChannelData(0);
            const buffer = new Int16Array(input.length);

            // 将浮点数据转换为16位整数
            for (let i = 0; i < input.length; i++) {
              buffer[i] = Math.max(
                -32768,
                Math.min(32767, Math.floor(input[i] * 32767))
              );
            }

            // 处理PCM数据
            this.processPCMBuffer(buffer);
          };

          // 需要连接输出，否则不会触发处理
          // 我们创建一个静音通道
          const silent = this.audioContext.createGain();
          silent.gain.value = 0;
          scriptProcessor.connect(silent);
          silent.connect(this.audioContext.destination);

          return { node: scriptProcessor, type: "processor" };
        }
      } catch (error) {
        console.log(
          `创建音频处理器失败: ${error.message}，尝试回退方案`,
          "error"
        );

        // 最后回退方案：使用ScriptProcessorNode
        try {
          const frameSize = 4096; // ScriptProcessorNode缓冲区大小
          const scriptProcessor = this.audioContext.createScriptProcessor(
            frameSize,
            1,
            1
          );

          scriptProcessor.onaudioprocess = (event) => {
            if (!this.isRecording) return;

            const input = event.inputBuffer.getChannelData(0);
            const buffer = new Int16Array(input.length);

            for (let i = 0; i < input.length; i++) {
              buffer[i] = Math.max(
                -32768,
                Math.min(32767, Math.floor(input[i] * 32767))
              );
            }

            this.processPCMBuffer(buffer);
          };

          const silent = this.audioContext.createGain();
          silent.gain.value = 0;
          scriptProcessor.connect(silent);
          silent.connect(this.audioContext.destination);

          console.log("使用ScriptProcessorNode作为回退方案成功", "warning");
          return { node: scriptProcessor, type: "processor" };
        } catch (fallbackError) {
          console.log(`回退方案也失败: ${fallbackError.message}`, "error");
          return null;
        }
      }
    },
    // 处理PCM缓冲数据
    processPCMBuffer(buffer) {
      if (!this.isRecording) return;

      // 将新的PCM数据追加到缓冲区
      const newBuffer = new Int16Array(
        this.pcmDataBuffer.length + buffer.length
      );
      newBuffer.set(this.pcmDataBuffer);
      newBuffer.set(buffer, this.pcmDataBuffer.length);
      this.pcmDataBuffer = newBuffer;

      // 检查是否有足够的数据进行Opus编码（16000Hz, 60ms = 960个采样点）
      const samplesPerFrame = 960; // 60ms @ 16kHz

      while (this.pcmDataBuffer.length >= samplesPerFrame) {
        // 从缓冲区取出一帧数据
        const frameData = this.pcmDataBuffer.slice(0, samplesPerFrame);
        this.pcmDataBuffer = this.pcmDataBuffer.slice(samplesPerFrame);

        // 编码为Opus
        this.encodeAndSendOpus(frameData);
      }
    },
    // 编码并发送Opus数据
    encodeAndSendOpus(pcmData = null) {
      if (!this.opusEncoder) {
        console.log("Opus编码器未初始化", "error");
        return;
      }

      try {
        // 如果提供了PCM数据，则编码该数据
        if (pcmData) {
          // 使用已初始化的Opus编码器编码
          const opusData = this.opusEncoder.encode(pcmData);

          if (opusData && opusData.length > 0) {
            // 存储音频帧
            this.audioBuffers.push(opusData.buffer);
            this.totalAudioSize += opusData.length;

            // 如果WebSocket已连接，则发送数据
            if (
              this.websocket &&
              this.websocket.readyState === WebSocket.OPEN
            ) {
              try {
                // 服务端期望接收原始Opus数据，不需要任何额外包装
                this.websocket.send(opusData.buffer);
                console.log(
                  `发送Opus帧，大小：${opusData.length}字节`,
                  "debug"
                );
              } catch (error) {
                console.log(`WebSocket发送错误: ${error.message}`, "error");
              }
            }
          } else {
            console.log("Opus编码失败，无有效数据返回", "error");
          }
        } else {
          // 处理剩余的PCM数据
          if (this.pcmDataBuffer.length > 0) {
            // 如果剩余的采样点不足一帧，用静音填充
            const samplesPerFrame = 960;
            if (this.pcmDataBuffer.length < samplesPerFrame) {
              const paddedBuffer = new Int16Array(samplesPerFrame);
              paddedBuffer.set(this.pcmDataBuffer);
              // 剩余部分为0（静音）
              this.encodeAndSendOpus(paddedBuffer);
            } else {
              this.encodeAndSendOpus(
                this.pcmDataBuffer.slice(0, samplesPerFrame)
              );
            }
            this.pcmDataBuffer = new Int16Array(0);
          }
        }
      } catch (error) {
        console.log(`Opus编码错误: ${error.message}`, "error");
      }
    },
    // 停止录音
    stopRecording() {
      if (!this.isRecording) return;

      try {
        // 使用直接PCM录音停止
        this.stopDirectRecording();
      } catch (error) {
        console.log(`停止录音错误: ${error.message}`, "error");
      }
    },
    // 停止直接从PCM数据录音
    stopDirectRecording() {
      if (!this.isRecording) return;

      try {
        // 停止录音
        this.isRecording = false;

        // 停止音频处理器的录音
        if (this.audioProcessor) {
          // 只有AudioWorklet才需要发送停止消息
          if (
            this.audioProcessorType === "worklet" &&
            this.audioProcessor.port
          ) {
            this.audioProcessor.port.postMessage({ command: "stop" });
          }

          this.audioProcessor.disconnect();
          this.audioProcessor = null;
        }

        // 断开音频连接
        if (this.audioSource) {
          this.audioSource.disconnect();
          this.audioSource = null;
        }

        // 停止可视化
        if (this.visualizationRequest) {
          cancelAnimationFrame(this.visualizationRequest);
          this.visualizationRequest = null;
        }

        // 清除录音计时器
        if (window.recordingTimer) {
          clearInterval(window.recordingTimer);
          window.recordingTimer = null;
        }

        // 编码并发送剩余的数据
        this.encodeAndSendOpus();

        // 发送一个空的消息作为结束标志（模拟接收到空音频数据的情况）
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
          // 使用空的Uint8Array发送最后一个空帧
          const emptyOpusFrame = new Uint8Array(0);
          this.websocket.send(emptyOpusFrame);

          // 发送监听结束消息
          const stopMessage = {
            type: "listen",
            mode: "manual",
            state: "stop",
          };

          this.websocket.send(JSON.stringify(stopMessage));
          console.log("已发送录音停止信号", "info");
        }

        // 重置UI
        this.$refs.recordButton.textContent = "开始录音";
        this.$refs.recordButton.classList.remove("recording");
        this.$refs.recordButton.style.disabled = false;

        console.log("停止PCM直接录音", "success");
        return true;
      } catch (error) {
        console.log(`直接录音停止错误: ${error.message}`, "error");
        return false;
      }
    },
    // 连接WebSocket服务器
    async connectToServer() {
      console.log("连接服务器");
      const url = this.serverUrl;
      try {
        // 获取并验证配置
        const config = this.getConfig();
        if (!this.validateConfig(config)) {
          return;
        }

        // 检查URL格式
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
          console.log("URL格式错误，必须以ws://或wss://开头", "error");
          return;
        }

        // 先检查OTA状态
        console.log("正在检查OTA状态...", "info");
        const otaUrl = this.otaUrl;

        try {
          const otaResponse = await fetch(otaUrl, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "Device-Id": config.deviceId,
              "Client-Id": config.clientId,
            },
            body: JSON.stringify({
              version: 0,
              uuid: "",
              application: {
                name: "xiaozhi-web-test",
                version: "1.0.0",
                compile_time: "2025-04-16 10:00:00",
                idf_version: "4.4.3",
                elf_sha256: "1234567890abcdef1234567890abcdef1234567890abcdef",
              },
              ota: {
                label: "xiaozhi-web-test",
              },
              board: {
                type: "xiaozhi-web-test",
                ssid: "xiaozhi-web-test",
                rssi: 0,
                channel: 0,
                ip: "192.168.1.1",
                mac: config.deviceMac,
              },
              flash_size: 0,
              minimum_free_heap_size: 0,
              mac_address: config.deviceMac,
              chip_model_name: "",
              chip_info: {
                model: 0,
                cores: 0,
                revision: 0,
                features: 0,
              },
              partition_table: [
                {
                  label: "",
                  type: 0,
                  subtype: 0,
                  address: 0,
                  size: 0,
                },
              ],
            }),
          });

          if (!otaResponse.ok) {
            throw new Error(
              `OTA检查失败: ${otaResponse.status} ${otaResponse.statusText}`
            );
          }

          const otaResult = await otaResponse.json();
          console.log(`OTA检查结果: ${JSON.stringify(otaResult)}`, "info");

          console.log("OTA检查通过，开始连接WebSocket...", "success");
          this.otaStatus = "ota已连接";
          document.getElementById("otaStatus").style.color = "green";
        } catch (error) {
          console.log(`OTA检查错误: ${error.message}`, "error");
          this.otaStatus = "ota未连接";
          document.getElementById("otaStatus").style.color = "red";
        }

        // 使用自定义WebSocket实现以添加认证头信息
        let connUrl = new URL(url);

        // 添加认证参数
        connUrl.searchParams.append("device-id", config.deviceId);
        connUrl.searchParams.append("client-id", config.clientId);

        console.log(`正在连接: ${connUrl.toString()}`, "info");
        this.websocket = new WebSocket(connUrl.toString());

        // 设置接收二进制数据的类型为ArrayBuffer
        this.websocket.binaryType = "arraybuffer";

        this.websocket.onopen = async () => {
          console.log(`已连接到服务器: ${url}`, "success");
          this.connectionStatus = "ws已连接";
          document.getElementById("connectionStatus").style.color = "green";

          // 连接成功后发送hello消息
          await this.sendHelloMessage();

          this.connect = "断开";
          this.isInput = false;
          this.isSendmessagebtn = false;
          this.isRecording = false;
          this.$refs.sendButton.style.background =
            "linear-gradient(135deg, #00b09b, #96c93d)";
          this.$refs.recordButton.style.background =
            "linear-gradient(135deg, #b74b95, #ff0000)";
          const audioInitialized = await this.initAudio();
          if (audioInitialized) {
            this.$refs.recordButton.style.disabled = false;
          }
        };

        this.websocket.onclose = () => {
          console.log("已断开连接", "info");
          this.connectionStatus = "ws已断开";
          document.getElementById("connectionStatus").style.color = "red";
          this.$refs.sendButton.style.background = "#cccccc";
          this.connect = "连接";

          // connectButton.onclick = connectToServer;
          this.isInput = true;
          this.isSendmessagebtn = true;
          this.$refs.recordButton.style.disabled = true;
          this.$refs.recordButton.style.background = "#cccccc";
          // stopButton.disabled = true;
        };

        this.websocket.onerror = (error) => {
          console.log(`WebSocket错误: ${error.message || "未知错误"}`, "error");
          this.connectionStatus = "ws未连接";
          document.getElementById("connectionStatus").style.color = "red";
        };

        this.websocket.onmessage = (event) => {
          try {
            // 检查是否为文本消息
            if (typeof event.data === "string") {
              const message = JSON.parse(event.data);

              if (message.type === "hello") {
                console.log(
                  `服务器回应：${JSON.stringify(message, null, 2)}`,
                  "success"
                );
              } else if (message.type === "tts") {
                // TTS状态消息
                if (message.state === "start") {
                  console.log("服务器开始发送语音", "info");
                } else if (message.state === "sentence_start") {
                  console.log(`服务器发送语音段: ${message.text}`, "info");
                  // 添加文本到会话记录
                  if (message.text) {
                    this.addMessage(message.text);
                  }
                } else if (message.state === "sentence_end") {
                  console.log(`语音段结束: ${message.text}`, "info");
                } else if (message.state === "stop") {
                  console.log("服务器语音传输结束", "info");
                  // 结束后更新UI状态
                  if (this.$refs.recordButton.style.disabled) {
                    this.$refs.recordButton.style.disabled = false;
                    this.$refs.recordButton.textContent = "开始录音";
                    this.$refs.recordButton.classList.remove("recording");
                  }
                }
              } else if (message.type === "audio") {
                // 音频控制消息
                console.log(
                  `收到音频控制消息: ${JSON.stringify(message)}`,
                  "info"
                );
              } else if (message.type === "stt") {
                // 语音识别结果
                console.log(`识别结果: ${message.text}`, "info");
                // 添加识别结果到会话记录
                this.messages.push({
                  sender: "user",
                  content: `${message.text}`,
                });
                this.$nextTick(() => {
                  const container = this.$refs.scrollContainer;
                  // 核心代码：滚动到底部
                  container.scrollTop = container.scrollHeight;
                });
                // this.addMessage(`[语音识别] ${message.text}`, true);
              } else if (message.type === "llm") {
                // 大模型回复
                console.log(`大模型回复: ${message.text}`, "info");
                // 添加大模型回复到会话记录
                if (
                  message.text &&
                  message.text !== "😊" &&
                  message.text !== "🙂"
                ) {
                  this.addMessage(message.text);
                }
              } else {
                // 未知消息类型
                console.log(`未知消息类型: ${message.type}`, "info");
                this.addMessage(JSON.stringify(message, null, 2));
              }
            } else {
              // 处理二进制数据 - 兼容多种二进制格式
              this.handleBinaryMessage(event.data);
            }
          } catch (error) {
            console.log(`WebSocket消息处理错误: ${error.message}`, "error");
            // 非JSON格式文本消息直接显示
            if (typeof event.data === "string") {
              this.addMessage(event.data);
            }
          }
        };

        this.connectionStatus = "ws未连接";
        document.getElementById("connectionStatus").style.color = "orange";
      } catch (error) {
        console.log(`连接错误: ${error.message}`, "error");
        this.connectionStatus = "ws未连接";
      }
    },
    // 发送hello握手消息
    async sendHelloMessage() {
      if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN)
        return;

      try {
        const config = this.getConfig();

        // 设置设备信息
        const helloMessage = {
          type: "hello",
          device_id: config.deviceId,
          device_name: config.deviceName,
          device_mac: config.deviceMac,
          token: config.token,
        };

        console.log("发送hello握手消息", "info");
        this.websocket.send(JSON.stringify(helloMessage));

        // 等待服务器响应
        return new Promise((resolve) => {
          // 5秒超时
          const timeout = setTimeout(() => {
            console.log("等待hello响应超时", "error");
            console.log('提示: 请尝试点击"测试认证"按钮进行连接排查', "info");
            resolve(false);
          }, 5000);

          // 临时监听一次消息，接收hello响应
          const onMessageHandler = (event) => {
            try {
              const response = JSON.parse(event.data);
              if (response.type === "hello" && response.session_id) {
                console.log(
                  `服务器握手成功，会话ID: ${response.session_id}`,
                  "success"
                );
                clearTimeout(timeout);
                this.websocket.removeEventListener("message", onMessageHandler);
                resolve(true);
              }
            } catch (e) {
              // 忽略非JSON消息
            }
          };

          this.websocket.addEventListener("message", onMessageHandler);
        });
      } catch (error) {
        console.log(`发送hello消息错误: ${error.message}`, "error");
        return false;
      }
    },
    // 断开WebSocket连接
    disconnectFromServer() {
      if (!this.websocket) return;

      this.websocket.close();
      this.stopRecording();
    },
    async handleBinaryMessage(data) {
      try {
        let arrayBuffer;

        // 根据数据类型进行处理
        if (data instanceof ArrayBuffer) {
          arrayBuffer = data;
          console.log(
            `收到ArrayBuffer音频数据，大小: ${data.byteLength}字节`,
            "debug"
          );
        } else if (data instanceof Blob) {
          // 如果是Blob类型，转换为ArrayBuffer
          arrayBuffer = await data.arrayBuffer();
          console.log(
            `收到Blob音频数据，大小: ${arrayBuffer.byteLength}字节`,
            "debug"
          );
        } else {
          console.log(`收到未知类型的二进制数据: ${typeof data}`, "warning");
          return;
        }

        // 创建Uint8Array用于处理
        const opusData = new Uint8Array(arrayBuffer);

        if (opusData.length > 0) {
          // 将数据添加到缓冲队列
          this.audioBufferQueue.push(opusData);

          // 如果收到的是第一个音频包，开始缓冲过程
          if (
            this.audioBufferQueue.length === 1 &&
            !this.isAudioBuffering &&
            !this.isAudioPlaying
          ) {
            this.startAudioBuffering();
          }
        } else {
          console.log("收到空音频数据帧，可能是结束标志", "warning");

          // 如果缓冲队列中有数据且没有在播放，立即开始播放
          if (this.audioBufferQueue.length > 0 && !this.isAudioPlaying) {
            this.playBufferedAudio();
          }

          // 如果正在播放，发送结束信号
          if (this.isAudioPlaying && this.streamingContext) {
            this.streamingContext.endOfStream = true;
          }
        }
      } catch (error) {
        console.log(`处理二进制消息出错: ${error.message}`, "error");
      }
    },
    // 获取配置值
    getConfig() {
      const deviceMac = this.savedMac;
      return {
        deviceId: deviceMac, // 使用MAC地址作为deviceId
        deviceName: "Web测试设备",
        deviceMac: deviceMac,
        clientId: "web_test_client",
        token: "your-token1",
      };
    },
    // 验证配置
    validateConfig(config) {
      if (!config.deviceMac) {
        console.log("设备MAC地址不能为空", "error");
        return false;
      }
      if (!config.clientId) {
        console.log("客户端ID不能为空", "error");
        return false;
      }
      return true;
    },
    // 生成随机MAC地址
    generateRandomMac() {
      const hexDigits = "0123456789ABCDEF";
      let mac = "";
      for (let i = 0; i < 6; i++) {
        if (i > 0) mac += ":";
        for (let j = 0; j < 2; j++) {
          mac += hexDigits.charAt(Math.floor(Math.random() * 16));
        }
      }
      return mac;
    },
    recordBtn() {
      if (this.isRecording) {
        this.stopRecording();
      } else {
        this.startRecording();
      }
    },
    // 开始录音
    startRecording() {
      if (this.isRecording) return;

      try {
        // 最小录音时长提示
        console.log("请至少录制1-2秒钟的音频，确保采集到足够数据", "info");

        // 获取服务器类型 - 从URL判断
        let isXiaozhiNative = false;

        // 检查是否是小智原生服务器 (根据URL特征判断)
        if (
          this.serverUrl.includes("xiaozhi") ||
          this.serverUrl.includes("localhost") ||
          this.serverUrl.includes("127.0.0.1")
        ) {
          isXiaozhiNative = true;
          console.log("检测到小智原生服务器，使用标准listen协议", "info");
        }

        // 使用直接PCM录音和libopus编码的方式
        this.startDirectRecording();
      } catch (error) {
        console.log(`录音启动错误: ${error.message}`, "error");
      }
    },
    // 开始直接从PCM数据录音
    async startDirectRecording() {
      if (this.isRecording) return;

      try {
        // 初始化Opus编码器
        if (!this.initOpusEncoder()) {
          console.log("无法启动录音: Opus编码器初始化失败", "error");
          return;
        }

        // 请求麦克风权限
        const stream = await navigator.mediaDevices.getUserMedia({
          audio: {
            echoCancellation: true,
            noiseSuppression: true,
            sampleRate: 16000,
            channelCount: 1,
          },
        });

        // 创建音频上下文和分析器
        if (!this.audioContext) {
          this.audioContext = new (window.AudioContext ||
            window.webkitAudioContext)({
            sampleRate: 16000,
            latencyHint: "interactive",
          });
        }

        // 创建音频处理器
        const processorResult = await this.createAudioProcessor();
        if (!processorResult) {
          console.log("无法创建音频处理器", "error");
          return;
        }
        console.log("成功创建音频处理器", "success");
        this.audioProcessor = processorResult.node;
        this.audioProcessorType = processorResult.type;

        // 连接音频处理链
        this.audioSource = this.audioContext.createMediaStreamSource(stream);
        this.analyser = this.audioContext.createAnalyser();
        this.analyser.fftSize = 2048;

        this.audioSource.connect(this.analyser);
        this.audioSource.connect(this.audioProcessor);

        // 启动录音
        this.pcmDataBuffer = new Int16Array();
        this.audioBuffers = [];
        this.totalAudioSize = 0;
        this.isRecording = true;
        console.log("执行 this.isRecording = ", this.isRecording);
        // 启动音频处理器的录音 - 只有AudioWorklet才需要发送消息
        if (this.audioProcessorType === "worklet" && this.audioProcessor.port) {
          this.audioProcessor.port.postMessage({ command: "start" });
        }

        // 发送监听开始消息
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
          // 使用与服务端期望的listen消息格式
          const listenMessage = {
            type: "listen",
            mode: "manual", // 使用手动模式，由我们控制开始/停止
            state: "start", // 表示开始录音
          };

          console.log(
            `发送录音开始消息: ${JSON.stringify(listenMessage)}`,
            "info"
          );
          this.websocket.send(JSON.stringify(listenMessage));
        } else {
          console.log("WebSocket未连接，无法发送开始消息", "error");
          return false;
        }

        // 开始音频可视化
        const dataArray = new Uint8Array(this.analyser.frequencyBinCount);
        this.drawVisualizer(dataArray);

        // 在UI上显示录音计时器
        let recordingSeconds = 0;
        const recordingTimer = setInterval(() => {
          recordingSeconds += 0.1;
          this.$refs.recordButton.textContent = `停止录音 ${recordingSeconds.toFixed(
            1
          )}秒`;
        }, 100);

        // 保存计时器，以便在停止时清除
        window.recordingTimer = recordingTimer;

        this.$refs.recordButton.classList.add("recording");
        this.$refs.recordButton.style.disabled = false;

        console.log("开始PCM直接录音", "success");
        return true;
      } catch (error) {
        console.log(`直接录音启动错误: ${error.message}`, "error");
        this.isRecording = false;
        return false;
      }
    },
    // 绘制音频可视化效果
    drawVisualizer(dataArray) {
      this.visualizationRequest = requestAnimationFrame(() =>
        this.drawVisualizer(dataArray)
      );

      if (!this.isRecording) return;

      this.analyser.getByteFrequencyData(dataArray);

      this.visualizerContext.fillStyle = "#fafafa";
      this.visualizerContext.fillRect(
        0,
        0,
        this.visualizerCanvas.width,
        this.visualizerCanvas.height
      );

      const barWidth = (this.visualizerCanvas.width / dataArray.length) * 2.5;
      let barHeight;
      let x = 0;

      for (let i = 0; i < dataArray.length; i++) {
        barHeight = dataArray[i] / 2;

        this.visualizerContext.fillStyle = `rgb(${barHeight + 100}, 50, 50)`;
        this.visualizerContext.fillRect(
          x,
          this.visualizerCanvas.height - barHeight,
          barWidth,
          barHeight
        );

        x += barWidth + 1;
      }
    },

    // 使用libopus创建一个Opus编码器
    initOpusEncoder() {
      try {
        if (this.opusEncoder) {
          return true; // 已经初始化过
        }

        if (!window.ModuleInstance) {
          console.log("无法创建Opus编码器：ModuleInstance不可用", "error");
          return false;
        }

        // 初始化一个Opus编码器
        const mod = window.ModuleInstance;

        const sampleRate = 16000; // 16kHz采样率
        const channels = 1; // 单声道
        const application = 2048; // OPUS_APPLICATION_VOIP = 2048

        // 创建编码器
        this.opusEncoder = {
          channels: channels,
          sampleRate: sampleRate,
          frameSize: 960, // 60ms @ 16kHz = 60 * 16 = 960 samples
          maxPacketSize: 4000, // 最大包大小
          module: mod,

          // 初始化编码器
          init1: () => {
            try {
              // 获取编码器大小
              const encoderSize = mod._opus_encoder_get_size(channels);
              console.log(`Opus编码器大小: ${encoderSize}字节`, "info");

              // 分配内存
              this.encoderPtr = mod._malloc(encoderSize);
              if (!this.encoderPtr) {
                throw new Error("无法分配编码器内存");
              }

              // 初始化编码器
              const err = mod._opus_encoder_init(
                this.encoderPtr,
                sampleRate,
                channels,
                application
              );

              if (err < 0) {
                throw new Error(`Opus编码器初始化失败: ${err}`);
              }

              // 设置位率 (16kbps)
              mod._opus_encoder_ctl(this.encoderPtr, 4002, 16000); // OPUS_SET_BITRATE

              // 设置复杂度 (0-10, 越高质量越好但CPU使用越多)
              mod._opus_encoder_ctl(this.encoderPtr, 4010, 5); // OPUS_SET_COMPLEXITY

              // 设置使用DTX (不传输静音帧)
              mod._opus_encoder_ctl(this.encoderPtr, 4016, 1); // OPUS_SET_DTX

              console.log("Opus编码器初始化成功", "success");
              return true;
            } catch (error) {
              if (this.encoderPtr) {
                mod._free(this.encoderPtr);
                this.encoderPtr = null;
              }
              console.log(`Opus编码器初始化失败: ${error.message}`, "error");
              return false;
            }
          },

          // 编码PCM数据为Opus
          encode: (pcmData) => {
            if (!this.encoderPtr) {
              if (!this.init1()) {
                return null;
              }
            }

            try {
              const mod = this.opusEncoder.module;
              // 为PCM数据分配内存
              const pcmPtr = mod._malloc(pcmData.length * 2); // 2字节/int16
              // 将PCM数据复制到HEAP
              for (let i = 0; i < pcmData.length; i++) {
                mod.HEAP16[(pcmPtr >> 1) + i] = pcmData[i];
              }

              // 为输出分配内存
              const outPtr = mod._malloc(this.opusEncoder.maxPacketSize);

              // 进行编码
              const encodedLen = mod._opus_encode(
                this.encoderPtr,
                pcmPtr,
                this.opusEncoder.frameSize,
                outPtr,
                this.opusEncoder.maxPacketSize
              );

              if (encodedLen < 0) {
                throw new Error(`Opus编码失败: ${encodedLen}`);
              }

              // 复制编码后的数据
              const opusData = new Uint8Array(encodedLen);
              for (let i = 0; i < encodedLen; i++) {
                opusData[i] = mod.HEAPU8[outPtr + i];
              }

              // 释放内存
              mod._free(pcmPtr);
              mod._free(outPtr);

              return opusData;
            } catch (error) {
              console.log(`Opus编码出错: ${error.message}`, "error");
              return null;
            }
          },

          // 销毁编码器
          destroy: () => {
            if (this.encoderPtr) {
              this.module._free(this.encoderPtr);
              this.encoderPtr = null;
            }
          },
        };

        const result = this.opusEncoder.init1();
        return result;
      } catch (error) {
        console.log(`创建Opus编码器失败: ${error.message}`, "error");
        return false;
      }
    },
    // 初始化Opus解码器 - 确保完全初始化完成后才返回
    async initOpusDecoder() {
      if (this.opusDecoder) return this.opusDecoder; // 已经初始化

      try {
        // 检查ModuleInstance是否存在
        if (typeof window.ModuleInstance === "undefined") {
          if (typeof Module !== "undefined") {
            // 使用全局Module作为ModuleInstance
            window.ModuleInstance = Module;
            console.log("使用全局Module作为ModuleInstance", "info");
          } else {
            throw new Error("Opus库未加载，ModuleInstance和Module对象都不存在");
          }
        }

        const mod = window.ModuleInstance;

        // 创建解码器对象
        this.opusDecoder = {
          channels: this.CHANNELS,
          rate: this.SAMPLE_RATE,
          frameSize: this.FRAME_SIZE,
          module: mod,
          decoderPtr: null, // 初始为null

          // 初始化解码器
          init2: () => {
            if (this.opusDecoder.decoderPtr) return true; // 已经初始化

            // 获取解码器大小
            const decoderSize = mod._opus_decoder_get_size(
              this.opusDecoder.channels
            );
            console.log(`Opus解码器大小: ${decoderSize}字节`, "debug");

            // 分配内存
            this.opusDecoder.decoderPtr = mod._malloc(decoderSize);
            if (!this.opusDecoder.decoderPtr) {
              throw new Error("无法分配解码器内存");
            }

            // 初始化解码器
            const err = mod._opus_decoder_init(
              this.opusDecoder.decoderPtr,
              this.opusDecoder.rate,
              this.opusDecoder.channels
            );

            if (err < 0) {
              this.opusDecoder.destroy(); // 清理资源
              throw new Error(`Opus解码器初始化失败: ${err}`);
            }

            console.log("Opus解码器初始化成功", "success");
            return true;
          },

          // 解码方法
          decode: (opusData) => {
            if (!this.opusDecoder.decoderPtr) {
              if (!this.init2()) {
                throw new Error("解码器未初始化且无法初始化");
              }
            }

            try {
              const mod = window.ModuleInstance;

              // 为Opus数据分配内存
              const opusPtr = mod._malloc(opusData.length);
              mod.HEAPU8.set(opusData, opusPtr);

              // 为PCM输出分配内存
              const pcmPtr = mod._malloc(this.opusDecoder.frameSize * 2); // Int16 = 2字节

              // 解码
              const decodedSamples = mod._opus_decode(
                this.opusDecoder.decoderPtr,
                opusPtr,
                opusData.length,
                pcmPtr,
                this.opusDecoder.frameSize,
                0 // 不使用FEC
              );

              if (decodedSamples < 0) {
                mod._free(opusPtr);
                mod._free(pcmPtr);
                throw new Error(`Opus解码失败: ${decodedSamples}`);
              }

              // 复制解码后的数据
              const decodedData = new Int16Array(decodedSamples);
              for (let i = 0; i < decodedSamples; i++) {
                decodedData[i] = mod.HEAP16[(pcmPtr >> 1) + i];
              }

              // 释放内存
              mod._free(opusPtr);
              mod._free(pcmPtr);

              return decodedData;
            } catch (error) {
              console.log(`Opus解码错误: ${error.message}`, "error");
              return new Int16Array(0);
            }
          },

          // 销毁方法
          destroy: () => {
            if (this.opusDecoder.decoderPtr) {
              this.module._free(this.opusDecoder.decoderPtr);
              this.opusDecoder.decoderPtr = null;
            }
          },
        };

        // 初始化解码器
        if (!this.opusDecoder.init2()) {
          throw new Error("Opus解码器初始化失败");
        }

        return this.opusDecoder;
      } catch (error) {
        console.log(`Opus解码器初始化失败: ${error.message}`, "error");
        this.opusDecoder = null; // 重置为null，以便下次重试
        throw error;
      }
    },
    // 初始化音频录制和处理
    async initAudio() {
      try {
        // 请求麦克风权限
        const stream = await navigator.mediaDevices.getUserMedia({
          audio: {
            echoCancellation: true,
            noiseSuppression: true,
            sampleRate: 16000, // 确保16kHz采样率
            channelCount: 1, // 确保单声道
          },
        });
        console.log("已获取麦克风访问权限", "success");

        // 创建音频上下文
        this.audioContext = new (window.AudioContext ||
          window.webkitAudioContext)({
          sampleRate: 16000, // 确保采样率与服务器期望的一致
          latencyHint: "interactive",
        });
        const source = this.audioContext.createMediaStreamSource(stream);

        // 获取实际音频轨道设置
        const audioTracks = stream.getAudioTracks();
        if (audioTracks.length > 0) {
          const track = audioTracks[0];
          const settings = track.getSettings();
          console.log(
            `实际麦克风设置 - 采样率: ${
              settings.sampleRate || "未知"
            }Hz, 声道数: ${settings.channelCount || "未知"}`,
            "info"
          );
        }

        // 创建分析器用于可视化
        this.analyser = this.audioContext.createAnalyser();
        this.analyser.fftSize = 2048;
        source.connect(this.analyser);

        // 尝试初始化MediaRecorder，按优先级尝试不同编码选项
        try {
          // 优先尝试使用Opus编码
          this.mediaRecorder = new MediaRecorder(stream, {
            mimeType: "audio/webm;codecs=opus",
            audioBitsPerSecond: 16000,
          });
          console.log("已初始化MediaRecorder (使用Opus编码)", "success");
          console.log(`选择的编码格式: ${this.mediaRecorder.mimeType}`, "info");
        } catch (e1) {
          try {
            // 如果Opus不支持，尝试MP3
            this.mediaRecorder = new MediaRecorder(stream, {
              mimeType: "audio/webm",
              audioBitsPerSecond: 16000,
            });
            console.log(
              "已初始化MediaRecorder (使用WebM标准编码，Opus不支持)",
              "warning"
            );
            console.log(
              `选择的编码格式: ${this.mediaRecorder.mimeType}`,
              "info"
            );
          } catch (e2) {
            try {
              // 尝试其他备选格式
              this.mediaRecorder = new MediaRecorder(stream, {
                mimeType: "audio/ogg;codecs=opus",
                audioBitsPerSecond: 16000,
              });
              console.log(
                "已初始化MediaRecorder (使用OGG+Opus编码)",
                "warning"
              );
              console.log(
                `选择的编码格式: ${console.mediaRecorder.mimeType}`,
                "info"
              );
            } catch (e3) {
              // 最后使用默认编码
              this.mediaRecorder = new MediaRecorder(stream);
              console.log(
                `已初始化MediaRecorder (使用默认编码: ${this.mediaRecorder.mimeType})`,
                "warning"
              );
            }
          }
        }

        // 处理录制的数据
        this.mediaRecorder.ondataavailable = (event) => {
          if (event.data.size > 0) {
            this.audioChunks.push(event.data);
          }
        };

        // 录制结束后处理数据
        this.mediaRecorder.onstop = async () => {
          // 停止可视化
          if (this.visualizationRequest) {
            cancelAnimationFrame(this.visualizationRequest);
            this.visualizationRequest = null;
          }

          console.log(
            `录音结束，已收集的音频块数量: ${this.audioChunks.length}`,
            "info"
          );
          if (this.audioChunks.length === 0) {
            console.log(
              "警告：没有收集到任何音频数据，请检查麦克风是否工作正常",
              "error"
            );
            return;
          }

          // 创建完整的录音blob
          const blob = new Blob(this.audioChunks, {
            type: this.audioChunks[0].type,
          });
          console.log(
            `已创建音频Blob，MIME类型: ${this.audioChunks[0].type}，大小: ${(
              blob.size / 1024
            ).toFixed(2)} KB`,
            "info"
          );

          // 保存原始块，以防清空后需要调试
          const chunks = [...this.audioChunks];
          this.audioChunks = [];

          try {
            // 将blob转换为ArrayBuffer
            const arrayBuffer = await blob.arrayBuffer();
            const uint8Array = new Uint8Array(arrayBuffer);

            console.log(
              `已转换为Uint8Array，准备发送，大小: ${(
                arrayBuffer.byteLength / 1024
              ).toFixed(2)} KB`,
              "info"
            );

            // 检查WebSocket状态
            if (!this.websocket) {
              console.log("错误：WebSocket连接不存在", "error");
              return;
            }

            if (this.websocket.readyState !== WebSocket.OPEN) {
              console.log(
                `错误：WebSocket连接未打开，当前状态: ${this.websocket.readyState}`,
                "error"
              );
              return;
            }

            // 直接发送二进制音频数据 - 这是最简单有效的方式
            try {
              // 注意：开始和结束消息已在录音开始和结束时发送
              // 这里只需要发送音频数据
              await new Promise((resolve) => setTimeout(resolve, 50));

              // 处理WebM容器格式，提取纯Opus数据
              // 服务器使用opuslib_next.Decoder，需要纯Opus帧
              console.log("正在处理音频数据，提取纯Opus帧...", "info");
              const opusData = extractOpusFrames(uint8Array);

              // 记录Opus数据大小
              console.log(
                `已提取Opus数据，大小: ${(opusData.byteLength / 1024).toFixed(
                  2
                )} KB`,
                "info"
              );

              // 发送音频消息第二步：二进制音频数据
              this.websocket.send(opusData);
              console.log(
                `已发送Opus音频数据: ${(opusData.byteLength / 1024).toFixed(
                  2
                )} KB`,
                "success"
              );
            } catch (error) {
              console.log(`音频数据发送失败: ${error.message}`, "error");

              // 尝试使用base64编码作为备选方案
              try {
                log("尝试使用base64编码方式发送...", "info");
                const base64Data = arrayBufferToBase64(arrayBuffer);
                const audioDataMessage = {
                  type: "audio",
                  action: "data",
                  format: "opus",
                  sample_rate: 16000,
                  channels: 1,
                  mime_type: chunks[0].type,
                  encoding: "base64",
                  data: base64Data,
                };
                this.websocket.send(JSON.stringify(audioDataMessage));
                console.log(
                  `已使用base64编码发送音频数据: ${(
                    arrayBuffer.byteLength / 1024
                  ).toFixed(2)} KB`,
                  "warning"
                );
              } catch (base64Error) {
                console.log(
                  `所有数据发送方式均失败: ${base64Error.message}`,
                  "error"
                );
              }
            }
          } catch (error) {
            console.log(`处理录音数据错误: ${error.message}`, "error");
          }
        };

        // 尝试初始化Opus解码器
        try {
          // 检查ModuleInstance是否存在（本地库导出的全局变量）
          if (typeof window.ModuleInstance === "undefined") {
            throw new Error("Opus库未加载，ModuleInstance对象不存在");
          }

          // 简单测试ModuleInstance是否可用
          if (
            typeof window.ModuleInstance._opus_decoder_get_size === "function"
          ) {
            const testSize = window.ModuleInstance._opus_decoder_get_size(1);
            console.log(
              `Opus解码器测试成功，解码器大小: ${testSize} 字节`,
              "success"
            );
          } else {
            throw new Error("Opus解码函数未找到");
          }
        } catch (err) {
          console.log(
            `Opus解码器初始化警告: ${err.message}，将在需要时重试`,
            "warning"
          );
        }

        console.log("音频系统初始化完成", "success");
        return true;
      } catch (error) {
        console.log(`音频初始化错误: ${error.message}`, "error");
        return false;
      }
    },
    // 发送消息
    sendMessage() {
      if (!this.userInput.trim()) {
        alert("请输入内容");
        return;
      }
      const message = this.userInput.trim();
      if (
        message === "" ||
        !this.websocket ||
        this.websocket.readyState !== WebSocket.OPEN
      )
        return;

      try {
        // 直接发送listen消息，不需要重复发送hello
        const listenMessage = {
          type: "listen",
          mode: "manual",
          state: "detect",
          text: message,
        };

        this.websocket.send(JSON.stringify(listenMessage));
        // 添加用户消息到会话记录
        // this.messages.push({
        //   sender: "user",
        //   content: this.userInput,
        // });
        console.log(`发送文本消息: ${message}`, "info");
      } catch (error) {
        console.log(`发送消息错误: ${error.message}`, "error");
      }

      // // 添加助手消息（包含建议）
      // this.messages.push({
      //   sender: "assistant",
      //   content: `您的问题已优化为："${
      //     this.suggestion
      //   }"\n\n基于此问题，我的建议是：${this.generateResponse(this.userInput)}`,
      // });

      // 重置输入
      this.resetInput();
    },

    // 生成回复内容
    generateResponse(input) {
      const responses = [
        "建议先明确核心目标，然后分解为具体可执行步骤。",
        "可以考虑采用分阶段实施的策略，每阶段设置明确里程碑。",
        "建议参考行业最佳实践，同时结合自身实际情况进行调整。",
        "实施前建议进行SWOT分析，全面评估优势、劣势、机会和威胁。",
        "关键成功因素包括：明确目标、团队协作、资源保障和持续改进。",
        "建议建立反馈机制，定期评估进展并根据反馈进行调整优化。",
      ];

      return responses[Math.floor(Math.random() * responses.length)];
    },

    // 重置输入
    resetInput() {
      this.userInput = "";
      this.suggestion = "";
      this.suggestionGenerated = false;
    },
    // 添加消息到会话记录
    addMessage(text, isUser = false) {
      this.messages.push({
        sender: "assistant",
        content: text,
      });
      this.$nextTick(() => {
        const container = this.$refs.scrollContainer;
        // 核心代码：滚动到底部
        container.scrollTop = container.scrollHeight;
      });
    },
    // 开始音频缓冲过程
    startAudioBuffering() {
      if (this.isAudioBuffering || this.isAudioPlaying) return;

      this.isAudioBuffering = true;
      console.log("开始音频缓冲...", "info");

      // 先尝试初始化解码器，以便在播放时已准备好
      this.initOpusDecoder().catch((error) => {
        console.log(`预初始化Opus解码器失败: ${error.message}`, "warning");
        // 继续缓冲，我们会在播放时再次尝试初始化
      });

      // 设置超时，如果在一定时间内没有收集到足够的音频包，就开始播放
      setTimeout(() => {
        if (this.isAudioBuffering && this.audioBufferQueue.length > 0) {
          console.log(
            `缓冲超时，当前缓冲包数: ${this.audioBufferQueue.length}，开始播放`,
            "info"
          );
          this.playBufferedAudio();
        }
      }, 300); // 300ms超时

      // 监控缓冲进度
      const bufferCheckInterval = setInterval(() => {
        if (!this.isAudioBuffering) {
          clearInterval(bufferCheckInterval);
          return;
        }

        // 当累积了足够的音频包，开始播放
        if (this.audioBufferQueue.length >= this.BUFFER_THRESHOLD) {
          clearInterval(bufferCheckInterval);
          console.log(
            `已缓冲 ${this.audioBufferQueue.length} 个音频包，开始播放`,
            "info"
          );
          this.playBufferedAudio();
        }
      }, 50);
    },
    // 播放已缓冲的音频
    playBufferedAudio() {
      if (this.isAudioPlaying || this.audioBufferQueue.length === 0) return;

      this.isAudioPlaying = true;
      this.isAudioBuffering = false;

      // 确保Opus解码器已初始化
      const initDecoderAndPlay = async () => {
        try {
          // 确保音频上下文存在
          if (!this.audioContext) {
            this.audioContext = new (window.AudioContext ||
              window.webkitAudioContext)({
              sampleRate: this.SAMPLE_RATE,
            });
            console.log(
              "创建音频上下文，采样率: " + this.SAMPLE_RATE + "Hz",
              "debug"
            );
          }

          // 确保解码器已初始化
          if (!this.opusDecoder) {
            console.log("初始化Opus解码器...", "info");
            try {
              this.opusDecoder = await this.initOpusDecoder();
              if (!this.opusDecoder) {
                throw new Error("解码器初始化失败");
              }
              console.log("Opus解码器初始化成功", "success");
            } catch (error) {
              console.log("Opus解码器初始化失败: " + error.message, "error");
              this.isAudioPlaying = false;
              return;
            }
          }

          // 创建流式播放上下文
          if (!this.streamingContext) {
            this.streamingContext = {
              queue: [], // 已解码的PCM队列
              playing: false, // 是否正在播放
              endOfStream: false, // 是否收到结束信号
              source: null, // 当前音频源
              totalSamples: 0, // 累积的总样本数
              lastPlayTime: 0, // 上次播放的时间戳

              // 将Opus数据解码为PCM
              decodeOpusFrames: async (opusFrames) => {
                if (!this.opusDecoder) {
                  console.log("Opus解码器未初始化，无法解码", "error");
                  return;
                }

                let decodedSamples = [];

                for (const frame of opusFrames) {
                  try {
                    // 使用Opus解码器解码
                    const frameData = this.opusDecoder.decode(frame);
                    if (frameData && frameData.length > 0) {
                      // 转换为Float32
                      const floatData = this.convertInt16ToFloat32(frameData);
                      decodedSamples.push(...floatData);
                    }
                  } catch (error) {
                    console.log("Opus解码失败: " + error.message, "error");
                  }
                }

                if (decodedSamples.length > 0) {
                  // 添加到解码队列
                  this.streamingContext.queue.push(...decodedSamples);
                  this.streamingContext.totalSamples += decodedSamples.length;

                  // 如果累积了至少0.2秒的音频，开始播放
                  const minSamples = this.SAMPLE_RATE * this.MIN_AUDIO_DURATION;
                  if (
                    !this.streamingContext.playing &&
                    this.streamingContext.queue.length >= minSamples
                  ) {
                    this.streamingContext.startPlaying();
                  }
                } else {
                  console.log("没有成功解码的样本", "warning");
                }
              },

              // 开始播放音频
              startPlaying: () => {
                if (
                  this.streamingContext.playing ||
                  this.streamingContext.queue.length === 0
                )
                  return;

                this.streamingContext.playing = true;

                // 创建新的音频缓冲区
                const minPlaySamples = Math.min(
                  this.streamingContext.queue.length,
                  this.SAMPLE_RATE
                ); // 最多播放1秒
                const currentSamples = this.streamingContext.queue.splice(
                  0,
                  minPlaySamples
                );

                const audioBuffer = this.audioContext.createBuffer(
                  this.CHANNELS,
                  currentSamples.length,
                  this.SAMPLE_RATE
                );
                audioBuffer.copyToChannel(new Float32Array(currentSamples), 0);

                // 创建音频源
                this.streamingContext.source =
                  this.audioContext.createBufferSource();
                this.streamingContext.source.buffer = audioBuffer;

                // 创建增益节点用于平滑过渡
                const gainNode = this.audioContext.createGain();

                // 应用淡入淡出效果避免爆音
                const fadeDuration = 0.02; // 20毫秒
                gainNode.gain.setValueAtTime(0, this.audioContext.currentTime);
                gainNode.gain.linearRampToValueAtTime(
                  1,
                  this.audioContext.currentTime + fadeDuration
                );

                const duration = audioBuffer.duration;
                if (duration > fadeDuration * 2) {
                  gainNode.gain.setValueAtTime(
                    1,
                    this.audioContext.currentTime + duration - fadeDuration
                  );
                  gainNode.gain.linearRampToValueAtTime(
                    0,
                    this.audioContext.currentTime + duration
                  );
                }

                // 连接节点并开始播放
                this.streamingContext.source.connect(gainNode);
                gainNode.connect(this.audioContext.destination);

                this.streamingContext.lastPlayTime =
                  this.audioContext.currentTime;
                console.log(
                  `开始播放 ${currentSamples.length} 个样本，约 ${(
                    currentSamples.length / this.SAMPLE_RATE
                  ).toFixed(2)} 秒`,
                  "info"
                );

                // 播放结束后的处理
                this.streamingContext.source.onended = () => {
                  this.streamingContext.source = null;
                  this.streamingContext.playing = false;

                  // 如果队列中还有数据或者缓冲区有新数据，继续播放
                  if (this.streamingContext.queue.length > 0) {
                    setTimeout(() => this.streamingContext.startPlaying(), 10);
                  } else if (this.audioBufferQueue.length > 0) {
                    // 缓冲区有新数据，进行解码
                    const frames = [...this.audioBufferQueue];
                    this.audioBufferQueue = [];
                    this.streamingContext.decodeOpusFrames(frames);
                  } else if (this.streamingContext.endOfStream) {
                    // 流已结束且没有更多数据
                    console.log("音频播放完成", "info");
                    this.isAudioPlaying = false;
                    this.streamingContext = null;
                  } else {
                    // 等待更多数据
                    setTimeout(() => {
                      // 如果仍然没有新数据，但有更多的包到达
                      if (
                        this.streamingContext.queue.length === 0 &&
                        this.audioBufferQueue.length > 0
                      ) {
                        const frames = [...this.audioBufferQueue];
                        this.audioBufferQueue = [];
                        this.streamingContext.decodeOpusFrames(frames);
                      } else if (
                        this.queue.length === 0 &&
                        this.audioBufferQueue.length === 0
                      ) {
                        // 真的没有更多数据了
                        console.log("音频播放完成 (超时)", "info");
                        this.isAudioPlaying = false;
                        this.streamingContext = null;
                      }
                    }, 500); // 500ms超时
                  }
                };

                this.streamingContext.source.start();
              },
            };
          }

          // 开始处理缓冲的数据
          const frames = [...this.audioBufferQueue];
          this.audioBufferQueue = []; // 清空缓冲队列

          // 解码并播放
          await this.streamingContext.decodeOpusFrames(frames);
        } catch (error) {
          console.log(`播放已缓冲的音频出错: ${error.message}`, "error");
          this.isAudioPlaying = false;
          this.streamingContext = null;
        }
      };

      // 执行初始化和播放
      initDecoderAndPlay();
    },
    // 将Int16音频数据转换为Float32音频数据
    convertInt16ToFloat32(int16Data) {
      const float32Data = new Float32Array(int16Data.length);
      for (let i = 0; i < int16Data.length; i++) {
        // 将[-32768,32767]范围转换为[-1,1]
        float32Data[i] = int16Data[i] / (int16Data[i] < 0 ? 0x8000 : 0x7fff);
      }
      return float32Data;
    },
  },
};
</script>
<style lang="scss" scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
}

body {
  background: linear-gradient(135deg, #1a2a6c, #b21f1f, #1a2a6c);
  min-height: 100vh;

  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  color: #333;
}

.container {
  width: 100%;
  width: 100vw;
  height: 100vh;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 20px;
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

header {
  background: linear-gradient(90deg, #4b6cb7, #182848);
  color: white;
  padding: 20px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 3px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logo i {
  font-size: 28px;
  color: #64ffda;
}

.logo h1 {
  font-size: 24px;
  font-weight: 600;
}

.tagline {
  font-size: 14px;
  opacity: 0.8;
  margin-top: 5px;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.conversation-container {
  flex: 3;
  display: flex;
  flex-direction: column;
  padding: 20px;
  border-right: 1px solid #eee;
  overflow: hidden;
}

.conversation-title {
  font-size: 18px;
  font-weight: 600;
  color: #182848;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  height: 543px;
  padding: 10px;
  background: rgba(245, 247, 250, 0.7);
  border-radius: 12px;
  margin-bottom: 15px;
  padding-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  max-width: 80%;
  padding: 15px;
  border-radius: 18px;
  position: relative;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  background: linear-gradient(135deg, #4b6cb7, #182848);
  color: white;
  align-self: flex-end;
  border-bottom-right-radius: 5px;
}

.assistant-message {
  background: #eef2f7;
  color: #333;
  align-self: flex-start;
  border-bottom-left-radius: 5px;
}

.message-header {
  font-weight: 600;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-content {
  line-height: 1.5;
}

.input-section {
  flex: 1;
  padding: 20px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  border-top: 1px solid #eee;
}

.input-title {
  font-size: 18px;
  font-weight: 600;
  color: #182848;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-container {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

textarea {
  flex: 1;
  padding: 15px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  resize: none;
  height: 120px;
  font-size: 16px;
  transition: border-color 0.3s;
}

textarea:focus {
  outline: none;
  border-color: #4b6cb7;
  box-shadow: 0 0 0 3px rgba(75, 108, 183, 0.2);
}

.buttons {
  display: flex;
  gap: 10px;
}

button {
  padding: 12px 25px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
}

.generate-btn {
  background: linear-gradient(135deg, #3a5ca5, #101e38);
  color: white;
}

.generate-btn:hover {
  background: linear-gradient(135deg, #3a5ca5, #101e38);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(75, 108, 183, 0.4);
}

.luyin-btn {
  background: #cccccc;
  // background: linear-gradient(135deg, #b74b95, #ff0000);
  color: white;
}

.luyin-btn:hover {
  background: linear-gradient(135deg, #b74b95, #ff0000);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(75, 108, 183, 0.4);
}

.send-btn {
  background: #cccccc;
  // background: linear-gradient(135deg, #00b09b, #96c93d);
  color: white;
}

.send-btn:hover {
  background: linear-gradient(135deg, #009e8a, #7db82d);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 176, 155, 0.4);
}

.cancel-btn {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.cancel-btn:hover {
  background: #e2e8f0;
}

.suggestion-section {
  padding: 20px;
  background: #f8fafc;
  border-radius: 12px;
  margin-top: 15px;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #182848;
  font-weight: 600;
}

.suggestion-box {
  padding: 15px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  min-height: 100px;
  max-height: 200px;
  max-width: 450px;
  overflow: auto;
  line-height: 1.6;
}

.confirmation {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #e6f7ff;
  border-radius: 10px;
  border-left: 4px solid #1890ff;
}

.highlight {
  background: linear-gradient(
    120deg,
    rgba(143, 224, 255, 0.4),
    rgba(143, 224, 255, 0.1)
  );
  background-repeat: no-repeat;
  background-size: 100% 40%;
  background-position: 0 88%;
}

.tips {
  background: #f0f9ff;
  padding: 15px;
  border-radius: 12px;
  margin-top: 20px;
  border-left: 4px solid #38b2ac;
}

.tips-title {
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #182848;
}

.tips ul {
  padding-left: 20px;
}

.tips li {
  margin-bottom: 8px;
  line-height: 1.5;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #4b6cb7;
  margin-top: 10px;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #4ade80;
}

@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
  }

  .conversation-container {
    border-right: none;
    border-bottom: 1px solid #eee;
  }

  .message {
    max-width: 90%;
  }
}
.section {
  margin-bottom: 20px;
  padding-top: 15px;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.section h2 {
  display: flex;
  margin-top: 0;
  color: black;
  font-size: 18px;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
}
.device-info {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: 20px;
  padding: 0 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
  height: 28px;
  line-height: 28px;
}

.device-info span {
  color: #666;
  font-size: 16px;
}

.device-info strong {
  color: #333;
  font-weight: 500;
}
.connection-controls {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  margin-bottom: 15px;
}

.luyin-btn.recording {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    background-color: #db4437;
  }

  50% {
    background-color: #ff6659;
  }

  100% {
    background-color: #db4437;
  }
}
.audio-visualizer {
  width: 0.1px;
  height: 0.1px;
  background: #cccccc;
}
</style>