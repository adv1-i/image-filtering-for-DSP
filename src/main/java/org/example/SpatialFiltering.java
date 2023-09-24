package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.List;
import java.awt.Point;

public class SpatialFiltering {
    /**
     * Типы шума, которые могут быть добавлены к изображению.
     */
    private enum NoiseType {
        UNIPOLAR,  // Униполярный шум
        BIPOLAR    // Биполярный шум
    }

    /**
     * Каналы цвета, которые могут быть извлечены из изображения.
     */
    private enum Channel {
        R,  // Красный канал
        G,  // Зеленый канал
        B   // Синий канал
    }

    /**
     * Типы фильтров, которые могут быть применены к изображению.
     */
    private enum FilterType {
        MEDIAN,         // Медианный фильтр
        HARMONIC_MEAN   // Среднегармонический фильтр
    }

    // Изображения
    private BufferedImage originalImage;   // Исходное изображение
    private BufferedImage noisyImage;      // Зашумленное изображение
    private BufferedImage channelImage;    // Изображение с извлеченным каналом
    private BufferedImage filteredImage;   // Отфильтрованное изображение

    // Метки для отображения изображений на форме
    private JLabel originalImageLabel;     // Метка для исходного изображения
    private JLabel noisyImageLabel;        // Метка для зашумленного изображения
    private JLabel channelImageLabel;      // Метка для изображения с извлеченным каналом
    private JLabel filteredImageLabel;     // Метка для отфильтрованного изображения


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new SpatialFiltering().createAndShowGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Создает и отображает графический интерфейс пользователя.
     *
     * @throws IOException Если возникла ошибка при чтении изображения.
     */
    private void createAndShowGUI() throws IOException {
        // Создаем окно
        JFrame frame = new JFrame("Image Noise");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);  // Развертываем окно на весь экран

        // Создаем панель для размещения элементов управления
        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.PAGE_START);  // Добавляем панель в верхнюю часть окна

        // Создаем кнопку для открытия изображения
        JButton openButton = new JButton("Открыть изображение");
        openButton.addActionListener(e -> openImage(frame));  // Добавляем обработчик события нажатия на кнопку
        panel.add(openButton);  // Добавляем кнопку на панель

        // Создаем выпадающий список для выбора типа шума
        JComboBox<NoiseType> noiseTypeComboBox = new JComboBox<>(NoiseType.values());
        panel.add(noiseTypeComboBox);  // Добавляем выпадающий список на панель

        // Создаем выпадающий список для выбора канала цвета
        JComboBox<Channel> channelComboBox = new JComboBox<>(Channel.values());
        panel.add(channelComboBox);  // Добавляем выпадающий список на панель


        // Создание кнопки "Применить шум и выделить канал"
        JButton applyButton = new JButton("Применить шум и выделить канал");

        // Добавление слушателя событий к кнопке
        applyButton.addActionListener(e -> {
            // Проверка, было ли уже загружено изображение
            if (originalImage != null) {
                // Создание копии оригинального изображения
                noisyImage = copyImage(originalImage);
                // Добавление шума к изображению
                addNoise(noisyImage, (NoiseType) noiseTypeComboBox.getSelectedItem());
                // Обновление метки изображения
                noisyImageLabel.setIcon(new ImageIcon(noisyImage));

                // Создание копии зашумленного изображения
                channelImage = copyImage(noisyImage);
                // Выделение канала из изображения
                extractChannel(channelImage, (Channel) channelComboBox.getSelectedItem());
                // Обновление метки изображения
                channelImageLabel.setIcon(new ImageIcon(channelImage));
            }
        });
        // Добавление кнопки на панель
        panel.add(applyButton);

        // Создание выпадающего списка для выбора типа фильтра
        JComboBox<FilterType> filterTypeComboBox = new JComboBox<>(FilterType.values());

        // Добавление выпадающего списка на панель
        panel.add(filterTypeComboBox);

        // Создание модели для спиннера с начальным значением 3, минимальным значением 3, максимальным значением 11 и шагом 2
        SpinnerNumberModel model = new SpinnerNumberModel(3, //initial value
                3, //min
                11, //max
                2); //step

        // Создание спиннера с заданной моделью и редактором для отображения значений в формате "0'x'"
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0'x'"));

        // Добавление спиннера на панель
        panel.add(spinner);



        // Создание кнопки "Применить фильтр"
        JButton applyNoiseFilterButton = new JButton("Применить фильтр");

        // Добавление слушателя событий к кнопке
        applyNoiseFilterButton.addActionListener(e -> {
            // Проверка, было ли уже загружено зашумленное изображение
            if (noisyImage != null) {
                // Создание копии зашумленного изображения
                filteredImage = copyImage(noisyImage);
                // Получение размера маски из спиннера
                int maskSize = (Integer) spinner.getValue();
                // Проверка выбранного типа фильтра и применение соответствующего фильтра
                if (filterTypeComboBox.getSelectedItem() == FilterType.MEDIAN) {
                    applyMedianFilter(filteredImage, maskSize);
                } else {  // FilterType.HARMONIC_MEAN
                    applyHarmonicMeanFilter(filteredImage, maskSize);
                }
                // Обновление метки изображения
                filteredImageLabel.setIcon(new ImageIcon(filteredImage));
            }
        });
        // Добавление кнопки на панель
        panel.add(applyNoiseFilterButton);

        // Создание панели для кнопок с выравниванием элементов по правому краю
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // Добавление панели кнопок в нижнюю часть основного окна
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Создание кнопки "Сохранить изображения"
        JButton saveAllButton = new JButton("Сохранить изображения");
        saveAllButton.addActionListener(e -> {
            // Создание диалога выбора файла
            JFileChooser fileChooser = new JFileChooser();

            // Проверка, было ли уже загружено зашумленное изображение
            if (noisyImage != null) {
                // Вывод сообщения о сохранении зашумленного изображения
                JOptionPane.showMessageDialog(frame, "Сохранение зашумленного изображения");
                // Установка предложенного имени файла
                fileChooser.setSelectedFile(new File("noise-image-01.png"));
                // Открытие диалога сохранения файла и проверка результата
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        // Сохранение изображения в файл
                        ImageIO.write(noisyImage, "png", file);
                        // Вывод сообщения об успешном сохранении файла
                        JOptionPane.showMessageDialog(frame, "Зашумленное изображение сохранено как " + file.getName());
                    } catch (IOException ioException) {
                        // Вывод информации об ошибке при сохранении файла
                        ioException.printStackTrace();
                    }
                }
            }

            // Проверка, было ли уже загружено зашумленное изображение по каналу
            if (channelImage != null) {
                // Вывод сообщения о сохранении зашумленного изображения по каналу
                JOptionPane.showMessageDialog(frame, "Сохранение зашумленного изображения по каналу");
                // Установка предложенного имени файла
                fileChooser.setSelectedFile(new File("channel-image-01.png"));
                // Открытие диалога сохранения файла и проверка результата
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        // Сохранение изображения в файл
                        ImageIO.write(channelImage, "png", file);
                        // Вывод сообщения об успешном сохранении файла
                        JOptionPane.showMessageDialog(frame, "Зашумленное изображение по каналу сохранено как " + file.getName());
                    } catch (IOException ioException) {
                        // Вывод информации об ошибке при сохранении файла
                        ioException.printStackTrace();
                    }
                }
            }

            // Проверка, было ли уже загружено отфильтрованное изображение
            if (filteredImage != null) {
                    // Вывод сообщения о сохранении отфильтрованного изображения
                    JOptionPane.showMessageDialog(frame, "Сохранение отфильтрованного изображения");
                    // Установка предложенного имени файла
                    fileChooser.setSelectedFile(new File("filter-image-01.png"));
                    // Открытие диалога сохранения файла и проверка результата
                    if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        try {
                            // Сохранение изображения в файл
                            ImageIO.write(filteredImage, "png", file);
                            // Вывод сообщения об успешном сохранении файла
                            JOptionPane.showMessageDialog(frame, "Отфильтрованное изображение сохранено как " + file.getName());
                        } catch (IOException ioException) {
                            // Вывод информации об ошибке при сохранении файла
                            ioException.printStackTrace();
                        }
                    }
                }
            });


            // Добавление кнопки "Сохранить изображения" на панель кнопок
            buttonPanel.add(saveAllButton);

        // Создание кнопки "Очистить изображения"
        JButton clearButton = new JButton("Очистить изображения");

        // Добавление слушателя событий к кнопке
        clearButton.addActionListener(e -> {
            // Удаление изображений из меток
            originalImageLabel.setIcon(null);
            noisyImageLabel.setIcon(null);
            channelImageLabel.setIcon(null);
            filteredImageLabel.setIcon(null);

            // Очистка ссылок на изображения
            originalImage = null;
            noisyImage = null;
            channelImage = null;
            filteredImage = null;
        });

        // Добавление кнопки на панель кнопок
        buttonPanel.add(clearButton);

        // Создание панели для изображений с расположением элементов в одну строку
        JPanel imagePanel = new JPanel(new GridLayout(1, 4));

        // Добавление панели изображений в центральную часть основного окна
        frame.getContentPane().add(imagePanel, BorderLayout.CENTER);

        // Создание меток для отображения изображений и добавление их на панель изображений с прокруткой
        originalImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(originalImageLabel));

        noisyImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(noisyImageLabel));

        channelImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(channelImageLabel));

        filteredImageLabel = new JLabel();
        imagePanel.add(new JScrollPane(filteredImageLabel));

        // Упаковка элементов окна и отображение окна
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Открывает изображение с помощью диалогового окна выбора файлов.
     *
     * @param frame - компонент для диалогового окна.
     */
    private void openImage(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser();

        // Создаем фильтр для файлов с расширениями png, jpg, jpeg и bmp
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image files", "png", "jpg", "jpeg", "bmp");
        fileChooser.setFileFilter(filter);

        // Отображаем диалоговое окно и получаем результат выбора пользователя
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            // Если пользователь подтвердил выбор файла
            File selectedFile = fileChooser.getSelectedFile();

            try {
                // Читаем изображение из выбранного файла
                originalImage = ImageIO.read(selectedFile);
                // Устанавливаем изображение в метку на форме
                originalImageLabel.setIcon(new ImageIcon(originalImage));
            } catch (IOException e) {
                // В случае ошибки чтения файла выводим информацию об ошибке
                e.printStackTrace();
            }
        }
    }


    /**
     * Добавляет шум к изображению.
     *
     * @param image Исходное изображение.
     * @param noiseType Тип шума (униполярный или биполярный).
     */
    private void addNoise(BufferedImage image, NoiseType noiseType) {
        Random rand = new Random();

        // Вычисляем общее количество пикселей в изображении
        int totalPixels = image.getWidth() * image.getHeight();
        System.out.println("Общее количество пикселей: " + totalPixels);

        // Вычисляем количество пикселей, которые нужно зашумить (10% от общего количества пикселей)
        int amount = (int) (totalPixels * 0.1);
        System.out.println("Количество шумовых пикселей: " + amount);
        System.out.println("Процент шумовых пикселей: " + (amount * 100.0 / totalPixels) + "%");

        List<Point> points = new ArrayList<>();

        // Создаем список всех пикселей на изображении
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                points.add(new Point(x, y));
            }
        }

        // Перемешиваем список
        Collections.shuffle(points, rand);

        // Берем первые 10% пикселей из перемешанного списка
        List<Point> noisePoints = points.subList(0, amount);

        // Добавляем шум к выбранным пикселям
        for (Point p : noisePoints) {
            Color color;
            if (noiseType == NoiseType.UNIPOLAR) {
                color = Color.WHITE;  // Светлая точка
            } else {  // Bipolar noise
                color = rand.nextBoolean() ? Color.BLACK : Color.WHITE;  // Случайно выбираем темную или светлую точку
            }

            // Устанавливаем цвет пикселя на изображении
            image.setRGB(p.x, p.y, color.getRGB());
        }
    }

    /**
     * Извлекает указанный канал из изображения.
     *
     * @param image Исходное изображение.
     * @param channel Канал, который нужно извлечь.
     */
    private void extractChannel(BufferedImage image, Channel channel) {
        // Проходим по каждому пикселю изображения
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // Получаем цвет текущего пикселя
                Color color = new Color(image.getRGB(x, y));

                // В зависимости от выбранного канала зануляем два других канала
                if (channel == Channel.R) {
                    color = new Color(color.getRed(), 0, 0);  // Оставляем только красный канал
                } else if (channel == Channel.G) {
                    color = new Color(0, color.getGreen(), 0);  // Оставляем только зеленый канал
                } else {  // Channel.B
                    color = new Color(0, 0, color.getBlue());  // Оставляем только синий канал
                }

                // Устанавливаем новый цвет для текущего пикселя в изображении
                image.setRGB(x, y, color.getRGB());
            }
        }
    }

    /**
     * Медианный фильтр.
     *
     * @param image    Изображение, к которому применяется фильтр.
     * @param maskSize Размер маски фильтра.
     */
    private void applyMedianFilter(BufferedImage image, int maskSize) {
        // Создаем копию изображения, чтобы не изменять исходное изображение при чтении пикселей
        BufferedImage copy = copyImage(image);

        // Вычисляем смещение маски относительно текущего пикселя
        int offset = maskSize / 2;

        // Проходим по всем пикселям изображения
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // Массивы для хранения значений RGB каждого пикселя в маске
                int[] reds = new int[maskSize * maskSize];
                int[] greens = new int[maskSize * maskSize];
                int[] blues = new int[maskSize * maskSize];

                // Проходим по всем пикселям в маске
                for (int ky = -offset; ky <= offset; ky++) {
                    for (int kx = -offset; kx <= offset; kx++) {
                        // Вычисляем координаты пикселя, учитывая границы изображения
                        int pixelX = Math.min(Math.max(x + kx, 0), image.getWidth() - 1);
                        int pixelY = Math.min(Math.max(y + ky, 0), image.getHeight() - 1);

                        // Читаем цвет пикселя
                        Color color = new Color(copy.getRGB(pixelX, pixelY));

                        // Сохраняем значения RGB пикселя в массивах
                        reds[(ky + offset) * maskSize + kx + offset] = color.getRed();
                        greens[(ky + offset) * maskSize + kx + offset] = color.getGreen();
                        blues[(ky + offset) * maskSize + kx + offset] = color.getBlue();
                    }
                }

                // Сортируем массивы значений RGB
                Arrays.sort(reds);
                Arrays.sort(greens);
                Arrays.sort(blues);

                // Вычисляем медианный цвет
                Color medianColor = new Color(reds[reds.length / 2], greens[greens.length / 2], blues[blues.length / 2]);

                // Устанавливаем медианный цвет для текущего пикселя
                image.setRGB(x, y, medianColor.getRGB());
            }
        }
    }


    /**
     * Среднегармонический фильтр.
     *
     * @param image    Изображение, к которому применяется фильтр.
     * @param maskSize Размер маски фильтра.
     */
    private void applyHarmonicMeanFilter(BufferedImage image, int maskSize) {
        // Создаем копию изображения, чтобы не изменять исходное изображение при чтении пикселей
        BufferedImage copy = copyImage(image);

        // Вычисляем смещение маски относительно текущего пикселя
        int offset = maskSize / 2;

        // Проходим по всем пикселям изображения
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {

                // Инициализируем суммы для каждого цвета
                double sumRed = 0, sumGreen = 0, sumBlue = 0;

                // Проходим по всем пикселям в маске
                for (int ky = -offset; ky <= offset; ky++) {
                    for (int kx = -offset; kx <= offset; kx++) {

                        // Вычисляем координаты пикселя, учитывая границы изображения
                        int pixelX = Math.min(Math.max(x + kx, 0), image.getWidth() - 1);
                        int pixelY = Math.min(Math.max(y + ky, 0), image.getHeight() - 1);

                        // Читаем цвет пикселя
                        Color color = new Color(copy.getRGB(pixelX, pixelY));

                        // Добавляем обратное значение каждого цвета в соответствующую сумму
                        sumRed += 1.0 / (color.getRed() + 1);
                        sumGreen += 1.0 / (color.getGreen() + 1);
                        sumBlue += 1.0 / (color.getBlue() + 1);
                    }
                }

                // Вычисляем гармоническое среднее для каждого цвета
                int meanRed = (int) ((maskSize * maskSize) / sumRed);
                int meanGreen = (int) ((maskSize * maskSize) / sumGreen);
                int meanBlue = (int) ((maskSize * maskSize) / sumBlue);

                // Ограничиваем значения цветов диапазоном [0, 255]
                meanRed = Math.min(Math.max(meanRed, 0), 255);
                meanGreen = Math.min(Math.max(meanGreen, 0), 255);
                meanBlue = Math.min(Math.max(meanBlue, 0), 255);

                // Создаем цвет на основе вычисленных средних значений
                Color meanColor = new Color(meanRed, meanGreen, meanBlue);

                // Устанавливаем вычисленный цвет для текущего пикселя
                image.setRGB(x, y, meanColor.getRGB());
            }
        }
    }

    /**
     * Копирование изображения.
     *
     * @param source Исходное изображение для копирования.
     * @return Копия исходного изображения.
     */
    private BufferedImage copyImage(BufferedImage source) {
        // Получаем цветовую модель исходного изображения
        ColorModel colorModel = source.getColorModel();

        // Проверяем, является ли альфа-канал премультиплицированным
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();

        // Копируем данные растра исходного изображения
        WritableRaster raster = source.copyData(null);

        // Создаем новое изображение с той же цветовой моделью, данными растра и информацией о премультипликации альфа-канала
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }
}



