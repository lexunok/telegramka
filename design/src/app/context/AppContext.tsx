import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

export interface User {
  id: string;
  name: string;
  phone: string;
}

export interface Message {
  id: string;
  chatId: string;
  senderId: string;
  text: string;
  timestamp: number;
}

export interface Chat {
  id: string;
  name: string;
  phone: string;
  lastMessage?: string;
  lastMessageTime?: number;
  unread?: number;
}

interface AppContextType {
  user: User | null;
  setUser: (user: User | null) => void;
  chats: Chat[];
  addChat: (chat: Chat) => void;
  messages: Message[];
  addMessage: (message: Message) => void;
  getMessagesForChat: (chatId: string) => Message[];
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('telegramka_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [chats, setChats] = useState<Chat[]>(() => {
    const saved = localStorage.getItem('telegramka_chats');
    return saved ? JSON.parse(saved) : [];
  });

  const [messages, setMessages] = useState<Message[]>(() => {
    const saved = localStorage.getItem('telegramka_messages');
    return saved ? JSON.parse(saved) : [];
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem('telegramka_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('telegramka_user');
    }
  }, [user]);

  useEffect(() => {
    localStorage.setItem('telegramka_chats', JSON.stringify(chats));
  }, [chats]);

  useEffect(() => {
    localStorage.setItem('telegramka_messages', JSON.stringify(messages));
  }, [messages]);

  const addChat = (chat: Chat) => {
    setChats(prev => {
      const exists = prev.find(c => c.id === chat.id);
      if (exists) return prev;
      return [...prev, chat];
    });
  };

  const addMessage = (message: Message) => {
    setMessages(prev => [...prev, message]);
    setChats(prev => prev.map(chat =>
      chat.id === message.chatId
        ? { ...chat, lastMessage: message.text, lastMessageTime: message.timestamp }
        : chat
    ));
  };

  const getMessagesForChat = (chatId: string) => {
    return messages.filter(m => m.chatId === chatId).sort((a, b) => a.timestamp - b.timestamp);
  };

  return (
    <AppContext.Provider value={{
      user,
      setUser,
      chats,
      addChat,
      messages,
      addMessage,
      getMessagesForChat,
    }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within AppProvider');
  }
  return context;
}
