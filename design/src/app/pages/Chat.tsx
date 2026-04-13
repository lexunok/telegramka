import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router';
import { useApp } from '../context/AppContext';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, Send, Smile } from 'lucide-react';

export default function Chat() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, chats, getMessagesForChat, addMessage } = useApp();
  const [messageText, setMessageText] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const chat = chats.find(c => c.id === id);
  const messages = id ? getMessagesForChat(id) : [];

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (!chat) {
    return (
      <div className="h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-xl text-foreground mb-4">Чат не найден</h2>
          <button
            onClick={() => navigate('/chats')}
            className="px-6 py-3 bg-primary text-white rounded-lg"
          >
            Вернуться к чатам
          </button>
        </div>
      </div>
    );
  }

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!messageText.trim() || !id || !user) return;

    const newMessage = {
      id: Date.now().toString(),
      chatId: id,
      senderId: user.id,
      text: messageText.trim(),
      timestamp: Date.now(),
    };

    addMessage(newMessage);
    setMessageText('');
  };

  const formatMessageTime = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="h-screen bg-background flex flex-col">
      <motion.header
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="bg-card border-b border-border px-4 py-3 flex items-center gap-3"
      >
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={() => navigate('/chats')}
          className="p-2 rounded-lg hover:bg-muted transition-colors"
        >
          <ArrowLeft className="w-5 h-5 text-foreground" />
        </motion.button>

        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center">
          <span className="text-white font-semibold">{chat.name.charAt(0).toUpperCase()}</span>
        </div>

        <div className="flex-1">
          <h2 className="font-semibold text-foreground">{chat.name}</h2>
          <p className="text-xs text-muted-foreground">{chat.phone}</p>
        </div>
      </motion.header>

      <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
        <AnimatePresence initial={false}>
          {messages.length === 0 ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex items-center justify-center h-full"
            >
              <div className="text-center">
                <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-3">
                  <Smile className="w-8 h-8 text-primary" />
                </div>
                <p className="text-muted-foreground">Начните разговор</p>
              </div>
            </motion.div>
          ) : (
            messages.map((message, index) => {
              const isOwn = message.senderId === user?.id;
              const showDate = index === 0 ||
                new Date(messages[index - 1].timestamp).toDateString() !== new Date(message.timestamp).toDateString();

              return (
                <div key={message.id}>
                  {showDate && (
                    <div className="flex justify-center mb-4">
                      <span className="px-3 py-1 bg-muted rounded-full text-xs text-muted-foreground">
                        {new Date(message.timestamp).toLocaleDateString('ru-RU', {
                          day: 'numeric',
                          month: 'long',
                          year: 'numeric'
                        })}
                      </span>
                    </div>
                  )}
                  <motion.div
                    initial={{ opacity: 0, y: 10, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ delay: index * 0.03 }}
                    className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-[70%] px-4 py-2 rounded-2xl ${
                        isOwn
                          ? 'bg-gradient-to-r from-primary to-accent text-white rounded-br-md'
                          : 'bg-card border border-border text-foreground rounded-bl-md'
                      }`}
                    >
                      <p className="break-words">{message.text}</p>
                      <div className="flex items-center justify-end gap-1 mt-1">
                        <span className={`text-xs ${isOwn ? 'text-white/70' : 'text-muted-foreground'}`}>
                          {formatMessageTime(message.timestamp)}
                        </span>
                      </div>
                    </div>
                  </motion.div>
                </div>
              );
            })
          )}
        </AnimatePresence>
        <div ref={messagesEndRef} />
      </div>

      <motion.footer
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="bg-card border-t border-border px-4 py-3"
      >
        <form onSubmit={handleSendMessage} className="flex items-end gap-2">
          <div className="flex-1 bg-input-background border border-border rounded-xl px-4 py-2 focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20 transition-all">
            <textarea
              value={messageText}
              onChange={(e) => setMessageText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSendMessage(e);
                }
              }}
              placeholder="Напишите сообщение..."
              rows={1}
              className="w-full bg-transparent text-foreground placeholder:text-muted-foreground resize-none focus:outline-none max-h-32"
              style={{ minHeight: '24px' }}
            />
          </div>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            type="submit"
            disabled={!messageText.trim()}
            className="w-12 h-12 rounded-xl bg-gradient-to-r from-primary to-accent text-white flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-lg hover:shadow-primary/30 transition-all"
          >
            <Send className="w-5 h-5" />
          </motion.button>
        </form>
      </motion.footer>
    </div>
  );
}
