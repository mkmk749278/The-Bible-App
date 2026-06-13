const verses = [
  { text: "For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life.", ref: "John 3:16" },
  { text: "I can do all this through him who gives me strength.", ref: "Philippians 4:13" },
  { text: "The Lord is my shepherd, I lack nothing.", ref: "Psalm 23:1" },
  { text: "Trust in the Lord with all your heart and lean not on your own understanding.", ref: "Proverbs 3:5" },
  { text: "Be strong and courageous. Do not be afraid; do not be discouraged, for the Lord your God will be with you wherever you go.", ref: "Joshua 1:9" },
  { text: "And we know that in all things God works for the good of those who love him, who have been called according to his purpose.", ref: "Romans 8:28" },
  { text: "Come to me, all you who are weary and burdened, and I will give you rest.", ref: "Matthew 11:28" },
];

function loadVerseOfTheDay() {
  const dayIndex = new Date().getDate() % verses.length;
  const verse = verses[dayIndex];
  document.getElementById("verse-text").textContent = `"${verse.text}"`;
  document.getElementById("verse-ref").textContent = `— ${verse.ref}`;
}

function search(query) {
  const q = query.toLowerCase();
  return verses.filter(
    (v) => v.text.toLowerCase().includes(q) || v.ref.toLowerCase().includes(q)
  );
}

document.getElementById("search-btn").addEventListener("click", () => {
  const query = document.getElementById("search-input").value.trim();
  const results = search(query);
  const container = document.getElementById("results");
  if (!query) { container.innerHTML = ""; return; }
  if (results.length === 0) {
    container.innerHTML = "<p>No verses found.</p>";
    return;
  }
  container.innerHTML = results
    .map((v) => `<blockquote>"${v.text}"<br/><cite>— ${v.ref}</cite></blockquote>`)
    .join("");
});

loadVerseOfTheDay();
