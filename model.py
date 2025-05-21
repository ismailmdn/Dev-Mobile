import numpy as np
import tensorflow as tf
from flask import Flask, request, jsonify
from collections import deque
import random
import uuid
from datetime import datetime

app = Flask(__name__)

# DQN Agent definition
class DQNAgent:
    def __init__(self, state_size, action_size):
        self.state_size = state_size
        self.action_size = action_size
        self.memory = deque(maxlen=2000)
        self.gamma = 0.95
        self.epsilon = 1.0
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.learning_rate = 0.001
        self.model = self._build_model()
        self.target_model = self._build_model()
        self.update_target_model()

    def _build_model(self):
        model = tf.keras.Sequential([
            tf.keras.layers.Dense(24, input_dim=self.state_size, activation='relu'),
            tf.keras.layers.Dense(24, activation='relu'),
            tf.keras.layers.Dense(self.action_size, activation='linear')
        ])
        model.compile(loss='mse', optimizer=tf.keras.optimizers.Adam(learning_rate=self.learning_rate))
        return model

    def update_target_model(self):
        self.target_model.set_weights(self.model.get_weights())

    def remember(self, state, action, reward, next_state, done):
        self.memory.append((state, action, reward, next_state, done))

    def act(self, state):
        if np.random.rand() <= self.epsilon:
            return random.randrange(self.action_size)
        act_values = self.model.predict(state.reshape(1, -1), verbose=0)
        return np.argmax(act_values[0])

    def replay(self, batch_size):
        minibatch = random.sample(self.memory, min(len(self.memory), batch_size))
        for state, action, reward, next_state, done in minibatch:
            target = reward
            if not done:
                target = reward + self.gamma * np.amax(self.target_model.predict(next_state.reshape(1, -1), verbose=0)[0])
            target_f = self.model.predict(state.reshape(1, -1), verbose=0)
            target_f[0][action] = target
            self.model.fit(state.reshape(1, -1), target_f, epochs=1, verbose=0)
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay

# Configuration
STATE_SIZE = 10
ACTION_SIZE = 5
agent = DQNAgent(STATE_SIZE, ACTION_SIZE)

# Action templates
ACTION_MEANINGS = {
    0: "Reduce your spending in the {category} category by {amount}€ this month to free up funds for other priorities.",
    1: "Increase your savings by {amount}€ to help reach your financial goal '{goal}' faster.",
    2: "Set a weekly spending limit of {amount}€ to better control your budget and avoid overspending.",
    3: "Challenge: Avoid any spending in the '{category}' category for {days} days to boost your savings.",
    4: "Automatic optimization: Adjust your expenses to increase your overall savings rate by {percent}%."
}

@app.route('/recommend', methods=['POST'])
def get_recommendation():
    try:
        data = request.json
        user_state = preprocess_data(data)
        num_recommendations = 3

        q_values = agent.model.predict(user_state.reshape(1, -1), verbose=0)[0]
        top_actions_indices = q_values.argsort()[-num_recommendations:][::-1]

        recommendations = []
        for action in top_actions_indices:
            recommendation_text = personalize_recommendation(action, data)
            confidence = float(q_values[action])
            recommendations.append({
                "action_type": int(action),
                "recommendation": recommendation_text,
                "confidence": confidence
            })

        recommendation_id = str(uuid.uuid4())

        return jsonify({
            "recommendation_id": recommendation_id,
            "user_id": data['user_id'],
            "timestamp": datetime.now().isoformat(),
            "recommendations": recommendations,
            "context": {
                "state": user_state.tolist(),
                "actions": [int(a) for a in top_actions_indices],
                "confidences": [float(q_values[a]) for a in top_actions_indices]
            }
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 400

@app.route('/feedback', methods=['POST'])
def process_feedback():
    try:
        data = request.json
        context = data['context']
        feedback = data['feedback']
        state = np.array(context['state'])

        # Determine which action to reward
        if 'action' in context:
            action = context['action']
            confidence = context.get('confidence', 1.0)
        else:
            action = context['actions'][0]
            confidence = context['confidences'][0]

        new_state = state  # Simplified; in a real system, use actual post-feedback state
        reward = calculate_reward(feedback, context)

        agent.remember(state, action, reward, new_state, False)
        agent.replay(32)

        return jsonify({"status": "success", "reward": float(reward)})

    except Exception as e:
        return jsonify({"error": str(e)}), 400

def preprocess_data(data):
    spending = [
        data['category_spending'].get('food', 0) / max(1, data['monthly_income']),
        data['category_spending'].get('transport', 0) / max(1, data['monthly_income']),
        data['category_spending'].get('entertainment', 0) / max(1, data['monthly_income']),
        data['category_spending'].get('health', 0) / max(1, data['monthly_income']),
        data['category_spending'].get('shopping', 0) / max(1, data['monthly_income']),
        data['category_spending'].get('utilities', 0) / max(1, data['monthly_income'])
    ]
    goals = [g['current'] / max(1, g['target']) for g in data['saving_goals'][:3]]
    goals += [0] * (3 - len(goals))
    savings_rate = data['monthly_savings'] / max(1, data['monthly_income'])
    return np.array([*spending, *goals, savings_rate])

def personalize_recommendation(action, data):
    if action == 0:
        category = max(data['category_spending'].items(), key=lambda x: x[1])[0]
        amount = min(30, max(5, int(data['category_spending'][category] * 0.15)))
        return ACTION_MEANINGS[action].format(category=category, amount=amount)
    elif action == 1:
        goal = max(data['saving_goals'], key=lambda g: g['target'] - g['current'])
        amount = min(100, max(10, (goal['target'] - goal['current']) / 6))
        return ACTION_MEANINGS[action].format(goal=goal['name'], amount=round(amount, 2))
    elif action == 2:
        weekly_limit = int(sum(data['category_spending'].values()) / 4 * 0.8)
        return ACTION_MEANINGS[action].format(amount=weekly_limit)
    elif action == 3:
        category = min(data['category_spending'].items(), key=lambda x: x[1])[0]
        days = random.choice([3, 5, 7])
        return ACTION_MEANINGS[action].format(category=category, days=days)
    elif action == 4:
        percent = round((data['monthly_savings'] / max(1, data['monthly_income'])) * 100 + 5, 1)
        return ACTION_MEANINGS[action].format(percent=percent)
    return "Generic recommendation"

def calculate_reward(feedback, context):
    if feedback == 'applied':
        return 1.0 + context.get('confidence', 1.0) * 0.5
    elif feedback == 'rejected':
        return -1.0
    elif feedback == 'ignored':
        return -0.3
    elif feedback == 'goal_achieved':
        return 2.0
    return 0.0

# Fixed: Correct main block
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
