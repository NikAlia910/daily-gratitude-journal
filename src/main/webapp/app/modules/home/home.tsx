import './home.scss';

import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Card, CardBody, CardTitle, Button, Container, Row, Col, Form, FormGroup, Input, Label, Badge, Alert } from 'reactstrap';

import { useAppSelector, useAppDispatch } from 'app/config/store';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { createEntity, getEntities, reset } from 'app/entities/gratitude-entry/gratitude-entry.reducer';
import { Mood } from 'app/shared/model/enumerations/mood.model';
import dayjs from 'dayjs';

export const Home = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const account = useAppSelector(state => state.authentication.account);
  const gratitudeEntries = useAppSelector(state => state.gratitudeEntry.entities);
  const loading = useAppSelector(state => state.gratitudeEntry.loading);
  const updateSuccess = useAppSelector(state => state.gratitudeEntry.updateSuccess);

  const [todayEntry, setTodayEntry] = useState('');
  const [selectedMood, setSelectedMood] = useState<Mood | null>(null);
  const [showSuccessMessage, setShowSuccessMessage] = useState(false);
  const [todayEntryExists, setTodayEntryExists] = useState(false);

  const today = dayjs().format('YYYY-MM-DD');
  const todayFormatted = dayjs().format('MMMM D, YYYY');

  const moodEmojis = {
    [Mood.HAPPY]: { emoji: '😊', label: 'Happy', color: '#FFC107' },
    [Mood.GRATEFUL]: { emoji: '🙏', label: 'Grateful', color: '#28A745' },
    [Mood.CONTENT]: { emoji: '😌', label: 'Content', color: '#6F42C1' },
    [Mood.HOPEFUL]: { emoji: '🌟', label: 'Hopeful', color: '#17A2B8' },
    [Mood.REFLECTIVE]: { emoji: '🤔', label: 'Reflective', color: '#FD7E14' },
    [Mood.OTHER]: { emoji: '💭', label: 'Other', color: '#6C757D' },
  };

  useEffect(() => {
    if (account?.login) {
      dispatch(getEntities({}));
    }
  }, [account]);

  useEffect(() => {
    if (updateSuccess) {
      setShowSuccessMessage(true);
      setTodayEntry('');
      setSelectedMood(null);
      dispatch(reset());
      dispatch(getEntities({}));
      setTimeout(() => setShowSuccessMessage(false), 3000);
    }
  }, [updateSuccess]);

  useEffect(() => {
    // Check if there's already an entry for today
    const existingTodayEntry = gratitudeEntries.find(entry => dayjs(entry.date).format('YYYY-MM-DD') === today);
    setTodayEntryExists(!!existingTodayEntry);
  }, [gratitudeEntries, today]);

  const handleSubmitGratitude = () => {
    if (todayEntry.trim()) {
      const newEntry = {
        date: dayjs(),
        entry: todayEntry.trim(),
        mood: selectedMood,
        timestamp: dayjs(),
      };
      dispatch(createEntity(newEntry));
    }
  };

  const getGreeting = () => {
    const hour = dayjs().hour();
    if (hour < 12) return 'Good Morning';
    if (hour < 17) return 'Good Afternoon';
    return 'Good Evening';
  };

  const recentEntries = gratitudeEntries.slice(0, 3).sort((a, b) => dayjs(b.date).valueOf() - dayjs(a.date).valueOf());

  const gratitudeStreak = calculateStreak(gratitudeEntries);

  if (!account?.login) {
    return (
      <Container className="gratitude-home">
        <Row className="justify-content-center">
          <Col lg="8" md="10">
            <div className="welcome-section text-center">
              <div className="hero-icon mb-4">
                <FontAwesomeIcon icon="heart" size="4x" />
              </div>
              <h1 className="display-4 mb-4">Daily Gratitude Journal</h1>
              <p className="lead mb-4">
                Cultivate mindfulness and emotional well-being through daily gratitude reflection. Start your journey towards a more
                grateful and fulfilling life.
              </p>
              <div className="mb-4" style={{ fontSize: '1rem', color: 'var(--text-secondary)' }}>
                ✨ Express daily gratitude • 🎯 Track your mood • 📈 Build lasting habits • 🔒 Private & secure
              </div>
              <div className="auth-buttons">
                <Link to="/account/register" className="btn btn-primary btn-lg me-3" data-cy="register">
                  <FontAwesomeIcon icon="sparkles" className="me-2" />
                  Start Your Journey
                </Link>
                <Link to="/login" className="btn btn-outline-primary btn-lg" data-cy="login">
                  <FontAwesomeIcon icon="sign-in-alt" className="me-2" />
                  Sign In
                </Link>
              </div>
              <div className="mt-4">
                <small className="text-muted">Join thousands of people building a gratitude practice</small>
              </div>
            </div>
          </Col>
        </Row>
      </Container>
    );
  }

  return (
    <Container className="gratitude-home">
      <Row>
        <Col lg="8" md="12">
          <div className="greeting-section mb-4">
            <h1 className="greeting-title">
              {getGreeting()}, {account.firstName || account.login}!
            </h1>
            <p className="greeting-subtitle">Today is {todayFormatted}</p>
            {gratitudeStreak > 0 ? (
              <Badge color="success" className="streak-badge">
                <FontAwesomeIcon icon="fire" className="me-1" />
                {gratitudeStreak} day{gratitudeStreak !== 1 ? 's' : ''} streak!
              </Badge>
            ) : (
              gratitudeEntries.length === 0 && (
                <div
                  className="alert alert-info border-0 mx-auto"
                  style={{ maxWidth: '500px', background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', color: '#0369a1' }}
                >
                  <FontAwesomeIcon icon="sparkles" className="me-2" />
                  Welcome to your gratitude journey! Start by writing your first entry below.
                </div>
              )
            )}
          </div>

          {showSuccessMessage && (
            <Alert color="success" className="success-message">
              <FontAwesomeIcon icon="check-circle" className="me-2" />
              Your gratitude entry has been saved! 🌟
            </Alert>
          )}

          {!todayEntryExists ? (
            <Card className="gratitude-entry-card mb-4">
              <CardBody>
                <CardTitle tag="h3" className="mb-4">
                  <FontAwesomeIcon icon="edit" className="me-2 text-primary" />
                  What are you grateful for today?
                </CardTitle>
                <div className="mb-3 text-muted" style={{ fontSize: '0.95rem', lineHeight: '1.6' }}>
                  💡 Take a moment to reflect on the positive moments, people, or experiences that brought you joy today.
                </div>
                <Form>
                  <FormGroup>
                    <Input
                      type="textarea"
                      value={todayEntry}
                      onChange={e => setTodayEntry(e.target.value)}
                      placeholder="I am grateful for the warm sunlight this morning, my friend's encouraging text, and the delicious coffee that started my day perfectly..."
                      rows="5"
                      className="gratitude-textarea"
                      data-cy="gratitudeText"
                      autoFocus
                    />
                    <div className="d-flex justify-content-between align-items-center mt-2">
                      <small className="text-muted">💭 There&apos;s no right or wrong way to express gratitude</small>
                      <small className="text-muted">{todayEntry.length} characters</small>
                    </div>
                  </FormGroup>

                  <FormGroup>
                    <Label className="mood-selector-label">
                      <FontAwesomeIcon icon="smile" className="me-2 text-primary" />
                      How are you feeling today?
                    </Label>
                    <div className="mb-2 text-muted" style={{ fontSize: '0.9rem' }}>
                      Choose the mood that best reflects your current state of mind (optional)
                    </div>
                    <div className="mood-selector">
                      {Object.entries(moodEmojis).map(([mood, config]) => (
                        <button
                          key={mood}
                          type="button"
                          className={`mood-button ${selectedMood === (mood as Mood) ? 'selected' : ''}`}
                          onClick={() => setSelectedMood(mood as Mood)}
                          style={{
                            borderColor: selectedMood === (mood as Mood) ? config.color : 'var(--border-light)',
                          }}
                          data-cy={`mood-${mood.toLowerCase()}`}
                          title={`Select ${config.label} mood`}
                        >
                          <span className="mood-emoji">{config.emoji}</span>
                          <span className="mood-label">{config.label}</span>
                        </button>
                      ))}
                    </div>
                    {selectedMood && (
                      <div className="text-center mt-2">
                        <small className="text-muted">
                          You&apos;re feeling <strong className="text-primary">{moodEmojis[selectedMood]?.label}</strong> today
                        </small>
                      </div>
                    )}
                  </FormGroup>

                  <div className="text-center">
                    <Button
                      color="primary"
                      size="lg"
                      onClick={handleSubmitGratitude}
                      disabled={!todayEntry.trim() || loading}
                      className="submit-button"
                      data-cy="saveGratitude"
                    >
                      <FontAwesomeIcon icon={loading ? 'spinner' : 'heart'} spin={loading} className="me-2" />
                      {loading ? 'Saving...' : 'Save My Gratitude'}
                    </Button>
                    {!todayEntry.trim() && !loading && (
                      <div className="mt-2">
                        <small className="text-muted">💭 Please write something you&apos;re grateful for before saving</small>
                      </div>
                    )}
                  </div>
                </Form>
              </CardBody>
            </Card>
          ) : (
            <Card className="today-complete-card mb-4">
              <CardBody className="text-center">
                <div className="complete-icon mb-3">
                  <FontAwesomeIcon icon="check-circle" size="3x" className="text-success" />
                </div>
                <h3 className="text-success mb-3">Today&apos;s Gratitude Recorded! 🎉</h3>
                <p className="text-muted mb-3">
                  You&apos;ve already captured what you&apos;re grateful for today. Come back tomorrow to continue your gratitude journey!
                </p>
                <Link to="/gratitude-entry" className="btn btn-outline-primary" data-cy="viewAllEntries">
                  <FontAwesomeIcon icon="book" className="me-2" />
                  View All Entries
                </Link>
              </CardBody>
            </Card>
          )}

          {recentEntries.length > 0 && (
            <Card className="recent-entries-card">
              <CardBody>
                <CardTitle tag="h4" className="mb-3">
                  <FontAwesomeIcon icon="history" className="me-2 text-secondary" />
                  Recent Reflections
                </CardTitle>
                {recentEntries.map((entry, index) => (
                  <div key={entry.id} className="recent-entry-item">
                    <div className="recent-entry-header">
                      <strong>{dayjs(entry.date).format('MMMM D, YYYY')}</strong>
                      {entry.mood && <span className="recent-entry-mood">{moodEmojis[entry.mood]?.emoji}</span>}
                    </div>
                    <p className="recent-entry-text">
                      {entry.entry?.substring(0, 150)}
                      {entry.entry && entry.entry.length > 150 ? '...' : ''}
                    </p>
                    {index < recentEntries.length - 1 && <hr className="recent-entry-divider" />}
                  </div>
                ))}
                <div className="text-center mt-3">
                  <Link to="/gratitude-entry" className="btn btn-outline-primary" data-cy="seeAllEntries">
                    <FontAwesomeIcon icon="arrow-right" className="me-2" />
                    See All Entries
                  </Link>
                </div>
              </CardBody>
            </Card>
          )}
        </Col>

        <Col lg="4" md="12">
          <div className="sidebar-content">
            <Card className="inspiration-card mb-4">
              <CardBody className="text-center">
                <div className="inspiration-icon mb-3">
                  <FontAwesomeIcon icon="quote-left" size="2x" />
                </div>
                <h6 className="mb-3" style={{ color: '#92400e', fontWeight: 600 }}>
                  Daily Inspiration
                </h6>
                <blockquote className="inspiration-quote">
                  &quot;Gratitude makes sense of our past, brings peace for today, and creates a vision for tomorrow.&quot;
                </blockquote>
                <cite className="inspiration-author">— Melody Beattie</cite>
                <div className="mt-3">
                  <small className="text-muted">✨ Let this guide your reflection today</small>
                </div>
              </CardBody>
            </Card>

            <Card className="stats-card">
              <CardBody>
                <CardTitle tag="h5" className="text-center mb-3">
                  <FontAwesomeIcon icon="chart-line" className="me-2 text-info" />
                  Your Journey
                </CardTitle>
                <div className="stats-grid">
                  <div className="stat-item">
                    <div className="stat-number">{gratitudeEntries.length}</div>
                    <div className="stat-label">Total Entries</div>
                  </div>
                  <div className="stat-item">
                    <div className="stat-number">{gratitudeStreak}</div>
                    <div className="stat-label">Day Streak</div>
                  </div>
                  <div className="stat-item">
                    <div className="stat-number">{gratitudeEntries.filter(e => dayjs().diff(dayjs(e.date), 'day') < 7).length}</div>
                    <div className="stat-label">This Week</div>
                  </div>
                  <div className="stat-item">
                    <div className="stat-number">{gratitudeEntries.filter(e => dayjs().diff(dayjs(e.date), 'day') < 30).length}</div>
                    <div className="stat-label">This Month</div>
                  </div>
                </div>
              </CardBody>
            </Card>
          </div>
        </Col>
      </Row>
    </Container>
  );
};

// Helper function to calculate streak
function calculateStreak(entries: any[]): number {
  if (!entries || entries.length === 0) return 0;

  const sortedDates = entries
    .map(e => dayjs(e.date).format('YYYY-MM-DD'))
    .sort()
    .reverse(); // Most recent first

  let streak = 0;
  let currentDate = dayjs();

  for (let i = 0; i < sortedDates.length; i++) {
    const entryDate = dayjs(sortedDates[i]);
    const diffDays = currentDate.diff(entryDate, 'day');

    if (diffDays === streak) {
      streak++;
      currentDate = entryDate;
    } else if (diffDays > streak) {
      break;
    }
  }

  return streak;
}

export default Home;
