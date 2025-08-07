<template>
  <div id="app">
    <div class="container">
      <header>
        <div class="logo">
          <i class="fas fa-robot"></i>
          <div>
            <h1>智能对话建议系统</h1>
            <div class="tagline">提出问题，获取优化建议，沟通更高效</div>
          </div>
        </div>
        <div class="system-status">
          <div class="status-indicator"></div>
          <span>系统运行中</span>
        </div>
      </header>

      <div class="main-content">
        <div class="conversation-container">
          <div class="conversation-title">
            <i class="fas fa-comments"></i>
            <span>对话记录</span>
          </div>
          <div class="messages">
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
                <span>{{ message.sender === "user" ? "您" : "智能助手" }}</span>
              </div>
              <div class="message-content">{{ message.content }}</div>
            </div>

            <div v-if="messages.length === 0" class="empty-conversation">
              <div class="message assistant-message">
                <div class="message-header">
                  <i class="fas fa-robot"></i>
                  <span>智能助手</span>
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
          <div class="input-title">
            <i class="fas fa-edit"></i>
            <span>输入您的问题</span>
          </div>
          <div class="input-container">
            <textarea
              v-model="userInput"
              placeholder="请输入您的问题，例如：'如何提高团队协作效率？'"
            ></textarea>
          </div>

          <div class="buttons">
            <button class="generate-btn" @click="generateAdvice">
              <i class="fas fa-magic"></i>
              生成建议
            </button>
            <button
              class="send-btn"
              :disabled="!suggestionGenerated"
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
export default {
  name: "chat",
  data() {
    return {
      userInput: "",
      suggestion: "",
      suggestionGenerated: false,
      messages: [],
    };
  },
  methods: {
    // 生成建议
    generateAdvice() {
      if (!this.userInput.trim()) {
        alert("请输入您的问题");
        return;
      }

      // 模拟API请求的延迟
      this.suggestion = "正在生成建议...";
      this.suggestionGenerated = true;

      setTimeout(() => {
        // 根据输入内容生成不同建议
        this.suggestion = this.generateSuggestion(this.userInput);
      }, 800);
    },

    // 根据问题生成建议的算法
    generateSuggestion(input) {
      const lowerInput = input.toLowerCase();

      // 根据不同问题类型生成不同建议
      if (lowerInput.includes("如何") || lowerInput.includes("怎样")) {
        return `关于"${input}"，建议改为："能否请您详细说明${input.substring(
          2
        )}的具体实施步骤或方法？" 这样表达更加清晰明确，有助于获得更具体的建议。`;
      }

      if (lowerInput.includes("为什么")) {
        return `关于"${input}"，建议改为："关于${input.substring(
          3
        )}的原因，能否请您从专业角度分析其主要影响因素？" 这种表述方式更加专业，有助于获得深度解答。`;
      }

      if (lowerInput.includes("?") || lowerInput.includes("？")) {
        return `关于"${input}"，建议改为："${input
          .replace("?", "")
          .replace(
            "？",
            ""
          )}，希望您能分享相关经验或专业见解。" 这种表述更开放，能获得更丰富的回答。`;
      }

      return `关于"${input}"，建议改为："针对${input}这一主题，能否请您提供专业建议或最佳实践方案？" 这种表述更专业，有助于获得高质量回答。`;
    },

    // 发送消息
    sendMessage() {
      if (!this.userInput.trim()) {
        alert("请输入内容");
        return;
      }

      // 添加用户消息
      this.messages.push({
        sender: "user",
        content: this.userInput,
      });

      // 添加助手消息（包含建议）
      this.messages.push({
        sender: "assistant",
        content: `您的问题已优化为："${
          this.suggestion
        }"\n\n基于此问题，我的建议是：${this.generateResponse(this.userInput)}`,
      });

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
  padding: 10px;
  background: rgba(245, 247, 250, 0.7);
  border-radius: 12px;
  margin-bottom: 15px;
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
  gap: 8px;
}

.generate-btn {
  background: linear-gradient(135deg, #4b6cb7, #182848);
  color: white;
}

.generate-btn:hover {
  background: linear-gradient(135deg, #3a5ca5, #101e38);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(75, 108, 183, 0.4);
}

.send-btn {
  background: linear-gradient(135deg, #00b09b, #96c93d);
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
</style>