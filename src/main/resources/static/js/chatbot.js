const chatbotContextElement = document.getElementById('chatbotContext');

if (chatbotContextElement) {
    let chatbotContext = {};

    try {
        chatbotContext = JSON.parse(chatbotContextElement.textContent || '{}');
    } catch (error) {
        chatbotContext = {
            pageContext: document.title,
            starters: [
                'What book am I most likely to enjoy?',
                'What book has the author Frank Herbert and the theme Science Fiction?',
                'Who wrote Harry Potter?'
            ]
        };
    }
    const starterContainer = document.getElementById('chatStarters');
    const messagesContainer = document.getElementById('chatMessages');
    const form = document.getElementById('chatForm');
    const input = document.getElementById('chatInput');
    const toggle = document.getElementById('chatToggle');
    const panel = document.getElementById('chatPanel');
    const closeButton = document.createElement('button');

    toggle.addEventListener('click', () => panel.classList.toggle('open'));
    document.addEventListener('click', event => {
        if (!event.target.closest('.chatbot-widget')) {
            panel.classList.remove('open');
        }
    });
    closeButton.type = 'button';
    closeButton.className = 'chat-close';
    closeButton.textContent = '×';
    closeButton.addEventListener('click', () => panel.classList.remove('open'));
    panel.prepend(closeButton);

    (chatbotContext.starters || []).forEach(starter => {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = starter;
        button.addEventListener('click', () => sendMessage(starter));
        starterContainer.appendChild(button);
    });

    form.addEventListener('submit', event => {
        event.preventDefault();
        const message = input.value.trim();

        if (message) {
            sendMessage(message);
        }
    });

    async function sendMessage(message) {
        addMessage(message, 'user');
        input.value = '';
        addMessage('Searching the RDF vector database...', 'bot');

        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({message, pageContext: chatbotContext.pageContext || document.title})
            });

            const data = await response.json();
            messagesContainer.lastElementChild.textContent = data.answer || 'I could not find an answer in the RDF vector database.';
        } catch (error) {
            messagesContainer.lastElementChild.textContent = 'The chatbot could not reach the server. Please make sure the Spring Boot app is running and try again.';
        }
    }

    function addMessage(text, type) {
        const message = document.createElement('p');
        message.className = `chat-message ${type}`;
        message.textContent = text;
        messagesContainer.appendChild(message);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}
