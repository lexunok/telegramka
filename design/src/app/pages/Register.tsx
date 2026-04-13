import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router';
import { useApp } from '../context/AppContext';
import { motion } from 'motion/react';
import { User as UserIcon, Phone, ArrowRight } from 'lucide-react';

export default function Register() {
  const location = useLocation();
  const phone = location.state?.phone || '';
  const [name, setName] = useState('');
  const navigate = useNavigate();
  const { setUser } = useApp();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !phone.trim()) return;

    const newUser = {
      id: 'current-user',
      name: name.trim(),
      phone: phone.trim(),
    };

    setUser(newUser);
    navigate('/chats');
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="w-full max-w-md"
      >
        <div className="text-center mb-12">
          <motion.div
            initial={{ scale: 0.8 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
            className="inline-block mb-6"
          >
            <div className="w-20 h-20 rounded-3xl bg-gradient-to-br from-primary to-accent flex items-center justify-center mx-auto shadow-lg shadow-primary/50">
              <UserIcon className="w-10 h-10 text-white" />
            </div>
          </motion.div>
          <h1 className="text-4xl font-bold text-foreground mb-2">Регистрация</h1>
          <p className="text-muted-foreground">Создайте свой профиль</p>
        </div>

        <motion.form
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          onSubmit={handleSubmit}
          className="space-y-6"
        >
          <div>
            <label htmlFor="name" className="block text-sm text-muted-foreground mb-2">
              Имя
            </label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Введите ваше имя"
              className="w-full px-4 py-4 bg-input-background border border-border rounded-xl text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
              autoFocus
            />
          </div>

          <div>
            <label htmlFor="phone" className="block text-sm text-muted-foreground mb-2">
              Номер телефона
            </label>
            <div className="relative">
              <input
                id="phone"
                type="tel"
                value={phone}
                readOnly
                className="w-full px-4 py-4 bg-muted border border-border rounded-xl text-muted-foreground cursor-not-allowed"
              />
              <Phone className="absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            </div>
          </div>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            type="submit"
            className="w-full bg-gradient-to-r from-primary to-accent text-white py-4 rounded-xl flex items-center justify-center gap-2 hover:shadow-lg hover:shadow-primary/30 transition-all"
          >
            Создать аккаунт
            <ArrowRight className="w-5 h-5" />
          </motion.button>
        </motion.form>
      </motion.div>
    </div>
  );
}
