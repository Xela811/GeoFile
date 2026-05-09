// 引入 CryptoJS（确保你已经 npm install crypto-js）
import CryptoJS from 'crypto-js';

// 定义 Worker 内部的 context
const ctx: Worker = self as any;

ctx.onmessage = async (e: MessageEvent<{ file: File }>) => {
  const { file } = e.data;
  const chunkSize = 50 * 1024 * 1024; // 50MB 每片，适合 Worker
  const chunks = Math.ceil(file.size / chunkSize);
  let currentChunk = 0;

  // 创建 Hash 实例
  const hasher = CryptoJS.algo.SHA256.create();

  const readNextChunk = (): Promise<void> => {
    return new Promise((resolve, reject) => {
      const start = currentChunk * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      const reader = new FileReader();

      reader.onload = (event) => {
        const result = event.target?.result;
        if (result instanceof ArrayBuffer) {
          // 转换为 WordArray
          const wordArray = CryptoJS.lib.WordArray.create(result);
          hasher.update(wordArray);
          currentChunk++;

          // 发送进度回主线程
          ctx.postMessage({
            type: 'progress',
            data: Math.round((currentChunk / chunks) * 100)
          });
          resolve();
        }
      };

      reader.onerror = () => reject(new Error('Chunk read failed'));
      reader.readAsArrayBuffer(file.slice(start, end));
    });
  };

  try {
    while (currentChunk < chunks) {
      await readNextChunk();
    }
    // 计算最终结果
    const hash = hasher.finalize().toString();
    ctx.postMessage({ type: 'success', data: hash });
  } catch (error: any) {
    ctx.postMessage({ type: 'error', data: error.message });
  }
};